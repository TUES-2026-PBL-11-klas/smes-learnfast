// LearnFast — Video (WebRTC via PeerJS)

let localStream = null;
let peer = null;
let currentCall = null;
let micEnabled = true;
let camEnabled = true;

(async function() {
    const user = await API.requireAuth();
    if (!user) return;

    const params = new URLSearchParams(window.location.search);
    const roomId = params.get('room');

    if (!roomId) {
        document.getElementById('video-status').textContent = 'No room specified. Go to Dashboard to start/join a session.';
        return;
    }

    try {
        // Get local media
        document.getElementById('video-status').textContent = 'Accessing camera and microphone...';
        localStream = await navigator.mediaDevices.getUserMedia({
            video: true,
            audio: true
        });
        document.getElementById('local-video').srcObject = localStream;

        // Create peer with room-based ID
        const peerId = `learnfast-${roomId}-${user.id}`;
        peer = new Peer(peerId);

        peer.on('open', (id) => {
            document.getElementById('video-status').textContent = 'Connected! Waiting for peer to join...';

            // Try to call the other person (we don't know their ID, so we try both patterns)
            // The trick: both users connect, one calls the other
            setTimeout(() => {
                // Try calling potential peer IDs
                tryCall(roomId, user.id);
            }, 2000);
        });

        peer.on('call', (call) => {
            document.getElementById('video-status').textContent = 'Incoming call...';
            call.answer(localStream);
            handleCall(call);
        });

        peer.on('error', (err) => {
            console.error('Peer error:', err);
            if (err.type === 'peer-unavailable') {
                document.getElementById('video-status').textContent = 'Waiting for peer to join...';
            }
        });

    } catch (err) {
        console.error('Media error:', err);
        document.getElementById('video-status').textContent =
            'Could not access camera/mic. Please grant permissions and try again.';
    }
})();

async function tryCall(roomId, myId) {
    // Get sessions to find the other user
    try {
        const sessions = await API.get('/api/sessions');
        const session = sessions.find(s => s.roomId === roomId);
        if (session) {
            const otherId = session.student.id === parseInt(myId) ?
                session.mentor.id : session.student.id;
            const otherPeerId = `learnfast-${roomId}-${otherId}`;

            const call = peer.call(otherPeerId, localStream);
            if (call) {
                handleCall(call);
            }
        }
    } catch(e) {
        console.error('Could not find peer:', e);
    }
}

function handleCall(call) {
    currentCall = call;

    call.on('stream', (remoteStream) => {
        document.getElementById('remote-video').srcObject = remoteStream;
        document.getElementById('remote-label').textContent = 'Connected';
        document.getElementById('video-status').textContent = '🟢 In call';
    });

    call.on('close', () => {
        document.getElementById('video-status').textContent = 'Call ended';
        document.getElementById('remote-label').textContent = 'Disconnected';
    });

    call.on('error', (err) => {
        console.error('Call error:', err);
    });
}

function toggleMic() {
    if (!localStream) return;
    micEnabled = !micEnabled;
    localStream.getAudioTracks().forEach(t => t.enabled = micEnabled);
    const btn = document.getElementById('btn-mic');
    btn.textContent = micEnabled ? '🎤' : '🔇';
    btn.classList.toggle('active', !micEnabled);
}

function toggleCamera() {
    if (!localStream) return;
    camEnabled = !camEnabled;
    localStream.getVideoTracks().forEach(t => t.enabled = camEnabled);
    const btn = document.getElementById('btn-camera');
    btn.textContent = camEnabled ? '📷' : '📷';
    btn.classList.toggle('active', !camEnabled);
}

function endCall() {
    if (currentCall) currentCall.close();
    if (localStream) localStream.getTracks().forEach(t => t.stop());
    if (peer) peer.destroy();
    window.location.href = '/frontend/pages/dashboard.html';
}
