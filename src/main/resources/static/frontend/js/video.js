// LearnFast — Video Call  (WebRTC via PeerJS)
// ?role=caller  → dials the callee
// ?role=callee  → waits and answers

let localStream   = null;
let peer          = null;
let currentCall   = null;
let retryTimer    = null;
let callTimer     = null;    // elapsed time counter
let callSeconds   = 0;
let micOn         = true;
let camOn         = true;
let connected     = false;
let pendingCall   = null;    // call attempt in flight
let myPeerId      = null;
let otherPeerId   = null;
let myRole        = null;
let myRoomId      = null;
let screenStream  = null;   // active screen capture stream
let screenOn      = false;

// Beacon fallback: if page closes without clicking End, mark as MISSED/ENDED
window.addEventListener('pagehide', () => {
    const params = new URLSearchParams(window.location.search);
    const cId    = parseInt(params.get('callId') || '0', 10);
    if (!cId) return;
    const status = connected ? 'ENDED' : 'MISSED';
    const body   = JSON.stringify({ status, durationSeconds: connected ? callSeconds : 0 });
    try { navigator.sendBeacon(`/api/calls/${cId}/status-beacon`, body); } catch (_) {}
});

const ICE = {
    config: {
        iceServers: [
            { urls: 'stun:stun.l.google.com:19302' },
            { urls: 'stun:stun1.l.google.com:19302' },
            { urls: 'stun:stun2.l.google.com:19302' },
            { urls: 'stun:stun3.l.google.com:19302' },
        ]
    }
};

(async function init() {
    const user = await API.requireAuth();
    if (!user) return;

    const params  = new URLSearchParams(window.location.search);
    const roomId  = params.get('room');
    const role    = params.get('role') || 'callee';
    const partner = params.get('partner') || '';
    const callId  = parseInt(params.get('callId') || '0', 10);

    if (!roomId) { setStatus('No room ID', 'error'); return; }

    myRole   = role;
    myRoomId = roomId;

    // Show partner name in waiting screen
    if (partner) {
        const el = document.getElementById('remote-name-label');
        const av = document.getElementById('remote-avatar');
        if (el) el.textContent = `Waiting for ${partner}…`;
        if (av) av.textContent = partner[0].toUpperCase();
    }

    setStatus('Requesting camera & mic…', '');

    try {
        localStream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true });
    } catch (e) {
        setStatus('Camera / mic denied', 'error');
        showError('Could not access your camera or microphone. Please allow access and try again.');
        return;
    }

    const localVideo = document.getElementById('local-video');
    if (localVideo) localVideo.srcObject = localStream;

    // ── PeerJS setup ──────────────────────────────────────────────────────────
    // Use a deterministic ID so caller and callee can find each other without
    // extra signalling. If the ID happens to still be lingering on the broker
    // from a previous session (common after reload), we retry with the same
    // id after a short delay.
    const baseMyId    = `lf-${roomId}-${role}`;
    const baseOtherId = `lf-${roomId}-${role === 'caller' ? 'callee' : 'caller'}`;
    otherPeerId       = baseOtherId;

    _connectPeer(baseMyId, /*isFallback*/ false);

    // Make local tile draggable (PiP)
    _makeDraggable(document.getElementById('tile-local'));
})();

// ── Create PeerJS instance and wire up handlers ───────────────────────────────
function _connectPeer(id, isFallback) {
    if (peer && !peer.destroyed) {
        try { peer.destroy(); } catch (_) {}
    }

    peer = new Peer(id, { debug: 0, ...ICE });
    myPeerId = id;

    peer.on('open', (openId) => {
        console.log('Peer open with id:', openId);
        if (myRole === 'caller') {
            setStatus('Ringing…', 'calling');
            _dial(otherPeerId);
            // Re-dial every 3s while waiting for callee to come online.
            clearInterval(retryTimer);
            retryTimer = setInterval(() => _dial(otherPeerId), 5000);
        } else {
            // Callee only LISTENS for an incoming peer.call(). The caller's
            // retry timer will keep re-dialing until we're online.
            // (Having both sides dial would create duplicate MediaConnections.)
            setStatus('Ready — waiting for caller', 'calling');
        }
    });

    peer.on('call', (call) => {
        console.log('Incoming peer call from', call.peer);
        // Answer with our local stream. Don't clear the retry timer yet —
        // we only consider ourselves connected once we receive a stream.
        try {
            call.answer(localStream);
        } catch (e) {
            console.error('answer failed', e);
            return;
        }
        _attach(call);
    });

    peer.on('error', (err) => {
        console.warn('PeerJS error:', err && err.type, err && err.message);
        if (!err) return;

        // peer-unavailable: the other side isn't connected yet. That's fine —
        // the retry timer will keep trying.
        if (err.type === 'peer-unavailable') return;

        // unavailable-id: our own deterministic ID is still lingering on the
        // broker from a previous session (typical after a page reload).
        // Wait a bit and re-create the peer with the SAME id so the other
        // side's deterministic dial still finds us.
        if (err.type === 'unavailable-id') {
            if (isFallback) {
                showError('Could not register peer ID. Please retry.');
                return;
            }
            console.log('ID still lingering — retrying in 2s with same id', id);
            setTimeout(() => _connectPeer(id, /*isFallback*/ true), 2000);
            return;
        }

        // network / server errors — try to reconnect once
        if (err.type === 'network' || err.type === 'disconnected' || err.type === 'server-error') {
            if (peer && !peer.destroyed && !peer.disconnected) {
                try { peer.reconnect(); } catch (_) {}
            }
            return;
        }

        if (!connected) showError(`Connection error (${err.type}). Click Retry.`);
    });

    peer.on('disconnected', () => {
        console.log('Peer disconnected — attempting reconnect');
        if (peer && !peer.destroyed) {
            try { peer.reconnect(); } catch (_) {}
        }
    });

    peer.on('close', () => {
        if (!connected) console.log('Peer closed before connection');
    });
}

// ── Dial (both sides retry — whoever is online first wins) ────────────────────
function _dial(otherId) {
    if (!peer || peer.destroyed || connected || !localStream) return;

    // Close the previous unanswered attempt to avoid duplicate streams.
    // (Safe: if the previous attempt had actually connected, `connected`
    // would already be true and we would have returned above.)
    if (pendingCall) {
        try { pendingCall.close(); } catch (_) {}
        pendingCall = null;
    }

    let call;
    try {
        call = peer.call(otherId, localStream);
    } catch (e) {
        console.warn('peer.call threw', e);
        return;
    }
    if (!call) return;

    pendingCall = call;

    call.on('stream', (remoteStream) => {
        if (connected) return;
        clearInterval(retryTimer);
        retryTimer = null;
        pendingCall = null;
        _attach(call);
        const rv = document.getElementById('remote-video');
        if (rv) rv.srcObject = remoteStream;
        _onConnected();
    });

    call.on('error', (err) => console.warn('dial error', err && err.type));
    call.on('close', () => {
        if (pendingCall === call) pendingCall = null;
    });
}

// ── Attach call events once we've picked up or originated a call ──────────────
function _attach(call) {
    currentCall = call;

    call.on('stream', (remoteStream) => {
        if (connected) return;
        clearInterval(retryTimer);
        retryTimer = null;
        const rv = document.getElementById('remote-video');
        if (rv) rv.srcObject = remoteStream;
        _onConnected();
    });

    call.on('close', () => {
        if (connected) {
            setStatus('Call ended', '');
            _hideWaiting(false);
        }
    });

    call.on('error', (err) => console.error('call error', err));
}

// ── On first stream received ──────────────────────────────────────────────────
function _onConnected() {
    connected = true;
    clearInterval(retryTimer);
    retryTimer = null;
    setStatus('In call', 'connected');
    _hideWaiting(true);

    // Start elapsed timer
    callSeconds = 0;
    if (callTimer) clearInterval(callTimer);
    callTimer = setInterval(() => {
        callSeconds++;
        const m = String(Math.floor(callSeconds / 60)).padStart(2, '0');
        const s = String(callSeconds % 60).padStart(2, '0');
        const el = document.getElementById('vc-timer');
        if (el) el.textContent = `${m}:${s}`;
    }, 1000);
}

function _hideWaiting(hide) {
    const w = document.getElementById('vc-waiting');
    const l = document.getElementById('remote-label');
    if (w) w.classList.toggle('hidden', hide);
    if (l) l.style.display = hide ? 'block' : 'none';
}

// ── Controls ──────────────────────────────────────────────────────────────────
function toggleMic() {
    if (!localStream) return;
    micOn = !micOn;
    localStream.getAudioTracks().forEach(t => t.enabled = micOn);
    const btn  = document.getElementById('btn-mic');
    const icon = document.getElementById('local-muted-icon');
    if (btn)  btn.classList.toggle('off', !micOn);
    if (btn)  btn.textContent = micOn ? '🎤' : '🔇';
    if (icon) icon.classList.toggle('visible', !micOn);
}

function toggleCamera() {
    if (!localStream) return;
    camOn = !camOn;
    localStream.getVideoTracks().forEach(t => t.enabled = camOn);
    const btn = document.getElementById('btn-cam');
    if (btn) btn.classList.toggle('off', !camOn);
    if (btn) btn.textContent = camOn ? '📷' : '🚫';
}

async function toggleScreenShare() {
    const btn = document.getElementById('btn-screen');

    if (screenOn) {
        _stopScreenShare();
        return;
    }

    if (!navigator.mediaDevices || !navigator.mediaDevices.getDisplayMedia) {
        alert('Screen sharing is not supported in this browser.');
        return;
    }

    try {
        screenStream = await navigator.mediaDevices.getDisplayMedia({ video: true, audio: false });
    } catch (e) {
        // User cancelled the picker — not an error
        return;
    }

    const screenTrack = screenStream.getVideoTracks()[0];

    // Replace the video track in the ongoing PeerJS call
    if (currentCall && currentCall.peerConnection) {
        const sender = currentCall.peerConnection.getSenders()
            .find(s => s.track && s.track.kind === 'video');
        if (sender) await sender.replaceTrack(screenTrack);
    }

    // Show screen in local PiP
    const localVideo = document.getElementById('local-video');
    if (localVideo) localVideo.srcObject = screenStream;

    // Update local tile label
    const localLabel = document.querySelector('#tile-local .vc-tile-label');
    if (localLabel) localLabel.textContent = '🖥️ Screen';

    screenOn = true;
    if (btn) btn.classList.add('sharing');

    // When user stops sharing via browser's own "Stop sharing" button
    screenTrack.addEventListener('ended', _stopScreenShare);
}

function _stopScreenShare() {
    if (!screenOn) return;
    screenOn = false;

    if (screenStream) {
        screenStream.getTracks().forEach(t => { try { t.stop(); } catch (_) {} });
        screenStream = null;
    }

    // Restore camera track
    const camTrack = localStream && localStream.getVideoTracks()[0];
    if (camTrack && currentCall && currentCall.peerConnection) {
        const sender = currentCall.peerConnection.getSenders()
            .find(s => s.track && s.track.kind === 'video');
        if (sender) sender.replaceTrack(camTrack);
    }

    // Restore local PiP to camera
    const localVideo = document.getElementById('local-video');
    if (localVideo && localStream) localVideo.srcObject = localStream;

    const localLabel = document.querySelector('#tile-local .vc-tile-label');
    if (localLabel) localLabel.textContent = 'You';

    const btn = document.getElementById('btn-screen');
    if (btn) btn.classList.remove('sharing');
}

async function endCall() {
    clearInterval(retryTimer); retryTimer = null;
    clearInterval(callTimer);  callTimer  = null;
    if (pendingCall) { try { pendingCall.close(); } catch (_) {} pendingCall = null; }
    if (currentCall) { try { currentCall.close(); } catch (_) {} currentCall = null; }
    if (screenStream) { screenStream.getTracks().forEach(t => { try { t.stop(); } catch (_) {} }); screenStream = null; }
    if (localStream) { localStream.getTracks().forEach(t => { try { t.stop(); } catch (_) {} }); }
    if (peer && !peer.destroyed) { try { peer.destroy(); } catch (_) {} }

    // Persist call record — await so the request completes before navigation
    const params   = new URLSearchParams(window.location.search);
    const cId      = parseInt(params.get('callId') || '0', 10);
    const wasConnected = connected;
    const dur      = wasConnected ? callSeconds : 0;
    if (cId) {
        try {
            await API.put(`/api/calls/${cId}/status`, {
                status: wasConnected ? 'ENDED' : 'MISSED',
                durationSeconds: dur
            });
        } catch (_) {}
    }

    window.location.href = '/frontend/pages/dashboard.html';
}

// ── Fullscreen ────────────────────────────────────────────────────────────────
function toggleFullscreen(tileId) {
    const tile = document.getElementById(tileId);
    if (!tile) return;
    const fs = document.fullscreenElement || document.webkitFullscreenElement;
    if (fs === tile) {
        (document.exitFullscreen || document.webkitExitFullscreen).call(document);
    } else {
        const req = tile.requestFullscreen || tile.webkitRequestFullscreen;
        if (req) req.call(tile);
    }
}

// Update fullscreen button icon when fullscreen state changes
document.addEventListener('fullscreenchange', _onFullscreenChange);
document.addEventListener('webkitfullscreenchange', _onFullscreenChange);
function _onFullscreenChange() {
    const fs = document.fullscreenElement || document.webkitFullscreenElement;
    document.querySelectorAll('.vc-tile-fullscreen').forEach(btn => {
        const tile = btn.closest('.vc-tile');
        btn.textContent = (fs && fs === tile) ? '✕' : '⛶';
    });
}

// ── Helpers ───────────────────────────────────────────────────────────────────
function setStatus(msg, state) {
    const el = document.getElementById('vc-status');
    if (!el) return;
    el.textContent = msg;
    el.className = 'vc-status-pill' + (state ? ` ${state}` : '');
}

function showError(msg) {
    const overlay = document.getElementById('vc-error-overlay');
    const txt     = document.getElementById('vc-error-msg');
    if (txt)     txt.textContent = msg;
    if (overlay) overlay.classList.add('visible');
    setStatus('Disconnected', 'error');
}

function _makeDraggable(el) {
    if (!el) return;
    let ox = 0, oy = 0;
    el.addEventListener('mousedown', (e) => {
        const rect = el.getBoundingClientRect();
        ox = e.clientX - rect.left; oy = e.clientY - rect.top;
        document.addEventListener('mousemove', onMove);
        document.addEventListener('mouseup', onUp);
    });
    function onMove(e) {
        el.style.right  = 'auto';
        el.style.bottom = 'auto';
        el.style.left   = (e.clientX - ox) + 'px';
        el.style.top    = (e.clientY - oy) + 'px';
    }
    function onUp() {
        document.removeEventListener('mousemove', onMove);
        document.removeEventListener('mouseup', onUp);
    }
}
