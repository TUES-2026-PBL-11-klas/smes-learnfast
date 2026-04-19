// LearnFast — Group Call (PeerJS mesh)
// Each joiner dials every existing participant; answers incoming calls from
// peers that join later. Presence is tracked server-side via REST + STOMP.

let gcLocalStream = null;
let gcPeer = null;
let gcMicOn = true;
let gcCamOn = true;
let gcScreenOn = false;
let gcScreenStream = null;
let gcMyPeerId = null;
let gcChannelId = null;
let gcCurrentUser = null;
let gcStompClient = null;
// peerId -> MediaConnection
let gcCalls = {};
// peerId -> userId (so we can clean up on PEER_LEFT which only carries userId)
let gcPeerToUser = {};

const ICE = { config: { iceServers: [
    { urls: 'stun:stun.l.google.com:19302' },
    { urls: 'stun:stun1.l.google.com:19302' },
]}};

(async function init() {
    const user = await API.requireAuth();
    if (!user) return;
    gcCurrentUser = user;

    const params = new URLSearchParams(window.location.search);
    gcChannelId = parseInt(params.get('channelId'), 10);
    const channelName = params.get('channelName') || 'Group Call';

    if (!gcChannelId) { setGcStatus('No channel', 'error'); return; }
    document.getElementById('gc-channel-name').textContent = '# ' + channelName;

    setGcStatus('Getting camera…', '');
    try {
        gcLocalStream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true });
    } catch (e) {
        console.warn('getUserMedia failed:', e);
        setGcStatus('Camera denied', 'error');
        return;
    }

    // Add local tile
    addTile('local', 'You (me)', gcLocalStream, true);

    // Connect PeerJS — unique ID per join so reloads don't collide on the broker
    gcMyPeerId = `lf-gc-${gcChannelId}-${user.id}-${Date.now()}`;
    gcPeer = new Peer(gcMyPeerId, { debug: 0, ...ICE });

    gcPeer.on('open', async () => {
        setGcStatus('Joining…', '');
        // Register on server; response contains the current roster (including us)
        const peers = await API.post(`/api/channels/${gcChannelId}/call/peers`, { peerId: gcMyPeerId });
        if (Array.isArray(peers)) {
            peers.forEach(p => {
                if (p.peerId && p.peerId !== gcMyPeerId) dialPeer(p.peerId, p.userId);
            });
        }
        const others = Array.isArray(peers) ? peers.filter(p => p.peerId !== gcMyPeerId).length : 0;
        setGcStatus(others > 0 ? 'In call' : 'Waiting for others…', others > 0 ? 'connected' : '');

        // Subscribe to call events for newcomers / leavers
        connectGcWs();
    });

    gcPeer.on('call', (call) => {
        call.answer(gcLocalStream);
        attachCall(call);
    });

    gcPeer.on('error', (err) => console.warn('GC peer error:', err && err.type));
})();

function dialPeer(peerId, userId) {
    if (!gcPeer || gcCalls[peerId]) return;
    const call = gcPeer.call(peerId, gcLocalStream);
    if (!call) return;
    gcCalls[peerId] = call;
    gcPeerToUser[peerId] = userId;
    call.on('stream', (stream) => addTile(peerId, 'User ' + userId, stream, false));
    call.on('close', () => removeTile(peerId));
    call.on('error', (e) => { console.warn('call err', e); removeTile(peerId); });
}

function attachCall(call) {
    gcCalls[call.peer] = call;
    call.on('stream', (stream) => addTile(call.peer, 'Participant', stream, false));
    call.on('close', () => removeTile(call.peer));
    call.on('error', (e) => { console.warn('call err', e); removeTile(call.peer); });
}

function addTile(id, label, stream, muted) {
    const stage = document.getElementById('gc-stage');
    if (!stage) return;
    if (document.getElementById('gctile-' + id)) return;
    const div = document.createElement('div');
    div.className = 'gc-tile';
    div.id = 'gctile-' + id;
    const video = document.createElement('video');
    video.autoplay = true;
    video.playsInline = true;
    video.muted = muted;
    video.srcObject = stream;
    const lbl = document.createElement('div');
    lbl.className = 'gc-tile-label';
    lbl.textContent = label;
    div.appendChild(video);
    div.appendChild(lbl);
    stage.appendChild(div);
    updateLayout();
}

function removeTile(peerId) {
    document.getElementById('gctile-' + peerId)?.remove();
    delete gcCalls[peerId];
    delete gcPeerToUser[peerId];
    updateLayout();
}

function updateLayout() {
    const tiles = document.querySelectorAll('.gc-tile');
    const n = tiles.length;
    const w = n <= 1 ? 600 : n <= 4 ? 360 : 260;
    tiles.forEach(t => t.style.width = w + 'px');
}

function connectGcWs() {
    const sock = new SockJS('/ws');
    gcStompClient = Stomp.over(sock);
    gcStompClient.debug = null;
    gcStompClient.connect({}, () => {
        gcStompClient.subscribe(`/topic/channel/${gcChannelId}/call`, (frame) => {
            const event = JSON.parse(frame.body);
            if (event.type === 'PEER_JOINED' && event.peerId && event.peerId !== gcMyPeerId) {
                dialPeer(event.peerId, event.userId);
                setGcStatus('In call', 'connected');
            } else if (event.type === 'PEER_LEFT') {
                // Find tile by userId (we stored peer->user mapping) and remove.
                Object.keys(gcPeerToUser).forEach(pid => {
                    if (gcPeerToUser[pid] === event.userId) {
                        const c = gcCalls[pid];
                        if (c) try { c.close(); } catch (_) {}
                        removeTile(pid);
                    }
                });
            }
        });
    });
}

async function gcToggleScreenShare() {
    const btn = document.getElementById('btn-screen');
    if (gcScreenOn) { _gcStopScreenShare(); return; }

    if (!navigator.mediaDevices?.getDisplayMedia) {
        alert('Screen sharing is not supported in this browser.');
        return;
    }
    try {
        gcScreenStream = await navigator.mediaDevices.getDisplayMedia({ video: true, audio: false });
    } catch (e) { return; } // user cancelled

    const screenTrack = gcScreenStream.getVideoTracks()[0];

    // Replace track in every active peer connection
    for (const call of Object.values(gcCalls)) {
        const sender = call.peerConnection?.getSenders().find(s => s.track?.kind === 'video');
        if (sender) await sender.replaceTrack(screenTrack);
    }

    // Show screen in local tile
    const localVideo = document.querySelector('#gctile-local video');
    if (localVideo) localVideo.srcObject = gcScreenStream;
    const localLabel = document.querySelector('#gctile-local .gc-tile-label');
    if (localLabel) localLabel.textContent = '🖥️ Screen';

    gcScreenOn = true;
    if (btn) btn.classList.add('sharing');
    screenTrack.addEventListener('ended', _gcStopScreenShare);
}

function _gcStopScreenShare() {
    if (!gcScreenOn) return;
    gcScreenOn = false;
    if (gcScreenStream) { gcScreenStream.getTracks().forEach(t => { try { t.stop(); } catch (_) {} }); gcScreenStream = null; }

    const camTrack = gcLocalStream?.getVideoTracks()[0];
    if (camTrack) {
        for (const call of Object.values(gcCalls)) {
            const sender = call.peerConnection?.getSenders().find(s => s.track?.kind === 'video');
            if (sender) sender.replaceTrack(camTrack);
        }
    }
    const localVideo = document.querySelector('#gctile-local video');
    if (localVideo && gcLocalStream) localVideo.srcObject = gcLocalStream;
    const localLabel = document.querySelector('#gctile-local .gc-tile-label');
    if (localLabel) localLabel.textContent = 'You (me)';

    const btn = document.getElementById('btn-screen');
    if (btn) btn.classList.remove('sharing');
}

function gcToggleMic() {
    if (!gcLocalStream) return;
    gcMicOn = !gcMicOn;
    gcLocalStream.getAudioTracks().forEach(t => t.enabled = gcMicOn);
    const btn = document.getElementById('btn-mic');
    if (btn) { btn.classList.toggle('off', !gcMicOn); btn.textContent = gcMicOn ? '🎤' : '🔇'; }
}

function gcToggleCam() {
    if (!gcLocalStream) return;
    gcCamOn = !gcCamOn;
    gcLocalStream.getVideoTracks().forEach(t => t.enabled = gcCamOn);
    const btn = document.getElementById('btn-cam');
    if (btn) { btn.classList.toggle('off', !gcCamOn); btn.textContent = gcCamOn ? '📷' : '🚫'; }
}

async function gcLeave() {
    try { await API.delete(`/api/channels/${gcChannelId}/call/peers`); } catch (_) {}
    if (gcPeer && !gcPeer.destroyed) try { gcPeer.destroy(); } catch (_) {}
    if (gcScreenStream) gcScreenStream.getTracks().forEach(t => { try { t.stop(); } catch (_) {} });
    if (gcLocalStream) gcLocalStream.getTracks().forEach(t => t.stop());
    if (gcStompClient) try { gcStompClient.disconnect(); } catch (_) {}
    window.location.href = '/frontend/pages/chat.html';
}

// Best-effort cleanup when the user closes the tab or navigates away.
// sendBeacon only supports POST, so we post a cleanup endpoint marker; the
// server treats it as a leave.
window.addEventListener('pagehide', () => {
    try {
        const blob = new Blob([JSON.stringify({ _method: 'DELETE' })], { type: 'application/json' });
        navigator.sendBeacon(`/api/channels/${gcChannelId}/call/peers`, blob);
    } catch (_) {}
});

function setGcStatus(msg, state) {
    const el = document.getElementById('gc-status');
    if (!el) return;
    el.textContent = msg;
    el.className = 'gc-status' + (state ? ' ' + state : '');
}
