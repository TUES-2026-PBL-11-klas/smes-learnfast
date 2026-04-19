// LearnFast — Call Manager
// Subscribes to /topic/call/{userId} on every authenticated page and shows an
// incoming-call modal when an INVITE arrives. Also handles outgoing calls via
// a REST POST that ensures the INVITE is pushed server-side even if the caller
// navigates away before STOMP had a chance to flush a client-sent signal.

let _stomp        = null;
let _me           = null;
let _declineTimer = null;
let _currentSignal = null;   // the invite signal currently on screen

// ─── CSS + overlay ────────────────────────────────────────────────────────────
(function bootstrap() {
    // Guard against duplicate inclusion on the same page.
    if (window.__lfCallManagerLoaded) return;
    window.__lfCallManagerLoaded = true;

    const style = document.createElement('style');
    style.textContent = `
    #call-overlay {
        display: none; position: fixed; inset: 0;
        background: rgba(0,0,0,0.78); backdrop-filter: blur(8px);
        z-index: 9999; align-items: center; justify-content: center;
    }
    .call-modal {
        background: #1e1f22;
        border: 1px solid rgba(255,255,255,0.1);
        border-radius: 20px; padding: 2.25rem 1.75rem;
        text-align: center; width: 300px;
        box-shadow: 0 32px 80px rgba(0,0,0,0.7);
        animation: callPop 0.28s cubic-bezier(.34,1.56,.64,1);
    }
    @keyframes callPop {
        from { transform: scale(0.8); opacity: 0; }
        to   { transform: scale(1);   opacity: 1; }
    }
    .call-ring {
        width: 76px; height: 76px; border-radius: 50%;
        background: linear-gradient(135deg, #5865f2, #7c6fff);
        display: flex; align-items: center; justify-content: center;
        font-size: 1.9rem; font-weight: 700; color: #fff;
        margin: 0 auto 1rem;
        box-shadow: 0 0 0 0 rgba(88,101,242,0.55);
        animation: pulse 1.4s ease-out infinite;
    }
    @keyframes pulse {
        0%   { box-shadow: 0 0 0 0   rgba(88,101,242,0.55); }
        70%  { box-shadow: 0 0 0 22px rgba(88,101,242,0);   }
        100% { box-shadow: 0 0 0 0   rgba(88,101,242,0);    }
    }
    .call-name     { font-size: 1.2rem; font-weight: 700; color: #fff; margin-bottom: 0.2rem; }
    .call-subtitle { font-size: 0.82rem; color: rgba(255,255,255,0.45); margin-bottom: 1.6rem; }
    .call-btns     { display: flex; gap: 1.5rem; justify-content: center; }
    .call-accept, .call-decline {
        width: 60px; height: 60px; border-radius: 50%; border: none;
        cursor: pointer; font-size: 1.5rem;
        display: flex; align-items: center; justify-content: center;
        transition: transform 0.12s, box-shadow 0.12s;
    }
    .call-accept  { background: #23a559; box-shadow: 0 4px 14px rgba(35,165,89,0.4); }
    .call-accept:hover  { transform: scale(1.12); box-shadow: 0 6px 20px rgba(35,165,89,0.6); }
    .call-decline { background: #ed4245; box-shadow: 0 4px 14px rgba(237,66,69,0.4); }
    .call-decline:hover { transform: scale(1.12); box-shadow: 0 6px 20px rgba(237,66,69,0.6); }
    .call-toast {
        position: fixed; bottom: 1.5rem; right: 1.5rem; z-index: 9998;
        background: #1e1f22; border: 1px solid rgba(255,255,255,0.1);
        border-radius: 10px; padding: 0.8rem 1.1rem;
        font-size: 0.88rem; color: #fff;
        animation: toastIn 0.25s ease;
    }
    @keyframes toastIn {
        from { transform: translateY(16px); opacity: 0; }
        to   { transform: translateY(0);    opacity: 1; }
    }
    `;
    document.head.appendChild(style);

    const overlay = document.createElement('div');
    overlay.id = 'call-overlay';
    document.body.appendChild(overlay);
})();

// ─── Init ─────────────────────────────────────────────────────────────────────
async function initCallManager() {
    // Don't run on the video page itself (it has its own PeerJS pipeline and
    // doesn't need a second SockJS connection).
    if (window.location.pathname.endsWith('/video.html')) return;

    if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') {
        console.warn('[call-manager] SockJS or Stomp not loaded — calls disabled on this page');
        return;
    }

    let me;
    try {
        me = await API.get('/api/auth/me');
    } catch (_) { return; }
    if (!me || me.error || !me.id) return;
    _me = me;

    try {
        const socket = new SockJS('/ws');
        _stomp = Stomp.over(socket);
        _stomp.debug = null;

        _stomp.connect({}, async () => {
            _stomp.subscribe(`/topic/call/${me.id}`, (msg) => {
                try { _handle(JSON.parse(msg.body)); }
                catch (e) { console.warn('Bad call signal', e); }
            });
            // Recover any missed INVITE (if the WS message arrived before we subscribed)
            try {
                const pending = await API.get('/api/calls/pending');
                if (Array.isArray(pending) && pending.length > 0) {
                    const c = pending[0];
                    _showIncoming({
                        type:       'INVITE',
                        roomId:     c.roomId,
                        callerId:   c.otherId,
                        callerName: c.otherName,
                        callId:     c.id
                    });
                }
            } catch (_) {}
        }, (err) => {
            console.warn('[call-manager] STOMP error:', err);
        });
    } catch (e) {
        console.warn('[call-manager] init failed:', e);
    }
}

// ─── Signal handler ───────────────────────────────────────────────────────────
function _handle(signal) {
    if (!signal || !signal.type) return;

    if (signal.type === 'INVITE')    { _showIncoming(signal); return; }

    if (signal.type === 'DECLINED')  {
        _toast(`📵 ${signal.calleeName || 'They'} declined the call`);
        return;
    }

    if (signal.type === 'CANCELLED') {
        clearTimeout(_declineTimer);
        _dismiss();
        _toast('📵 Caller cancelled');
        return;
    }
}

// ─── Incoming call UI ─────────────────────────────────────────────────────────
function _showIncoming(signal) {
    // Defensive validation — never show a modal with missing fields.
    if (!signal || !signal.roomId || !signal.callerId) {
        console.warn('[call-manager] Ignoring malformed INVITE', signal);
        return;
    }

    _currentSignal = signal;

    const overlay = document.getElementById('call-overlay');
    if (!overlay) return;

    const callerName = signal.callerName || 'Someone';
    const initial = callerName[0].toUpperCase();

    // Build DOM manually so we don't have to escape user input into an
    // onclick= attribute (previous bug source).
    overlay.innerHTML = '';
    const modal = document.createElement('div');
    modal.className = 'call-modal';
    modal.innerHTML = `
        <div class="call-ring"></div>
        <div class="call-name"></div>
        <div class="call-subtitle">Incoming video call…</div>
        <div class="call-btns">
            <button class="call-accept" title="Accept">📞</button>
            <button class="call-decline" title="Decline">📵</button>
        </div>
    `;
    modal.querySelector('.call-ring').textContent = initial;
    modal.querySelector('.call-name').textContent = callerName;
    modal.querySelector('.call-accept').addEventListener('click', () => _accept());
    modal.querySelector('.call-decline').addEventListener('click', () => _decline());
    overlay.appendChild(modal);
    overlay.style.display = 'flex';

    clearTimeout(_declineTimer);
    // Auto-miss after 30 s
    _declineTimer = setTimeout(() => {
        const s = _currentSignal;
        _dismiss();
        if (s && s.callId) _updateRecord(s.callId, 'MISSED', 0);
    }, 30000);
}

function _accept() {
    const s = _currentSignal;
    if (!s) return;
    clearTimeout(_declineTimer);
    _dismiss();
    if (s.callId) _updateRecord(s.callId, 'ACCEPTED', 0);
    const name = encodeURIComponent(s.callerName || '');
    window.location.href =
        `/frontend/pages/video.html?room=${encodeURIComponent(s.roomId)}` +
        `&role=callee&partner=${name}&callId=${s.callId || 0}`;
}

function _decline() {
    const s = _currentSignal;
    if (!s) return;
    clearTimeout(_declineTimer);
    _dismiss();
    if (s.callId) _updateRecord(s.callId, 'DECLINED', 0);
}

function _dismiss() {
    const ov = document.getElementById('call-overlay');
    if (ov) ov.style.display = 'none';
    _currentSignal = null;
}

// ─── Outgoing call ────────────────────────────────────────────────────────────
async function sendCallInvite(roomId, targetUserId, targetName) {
    // POST creates the DB record AND pushes INVITE to callee via server-side WebSocket.
    // No STOMP needed on caller side — eliminates the connection-race bug.
    let callId = 0;
    try {
        const res = await API.post('/api/calls', { calleeId: targetUserId, roomId });
        if (res && res.error) {
            if (typeof API.showAlert === 'function') API.showAlert(res.error, 'error');
            return;
        }
        if (res && res.callId) callId = res.callId;
    } catch (e) {
        console.warn('Could not create call record', e);
        if (typeof API.showAlert === 'function') API.showAlert('Could not start call', 'error');
        return;
    }

    const name = encodeURIComponent(targetName || '');
    window.location.href =
        `/frontend/pages/video.html?room=${encodeURIComponent(roomId)}` +
        `&role=caller&partner=${name}&callId=${callId}`;
}

// ─── API helpers ──────────────────────────────────────────────────────────────
async function _updateRecord(callId, status, durationSeconds) {
    if (!callId) return;
    try {
        await API.put(`/api/calls/${callId}/status`, { status, durationSeconds });
    } catch (e) { console.warn('Could not update call record', e); }
}

function _toast(message) {
    const t = document.createElement('div');
    t.className = 'call-toast';
    t.textContent = message;
    document.body.appendChild(t);
    setTimeout(() => t.remove(), 4000);
}

// Expose for inline onclick callers (e.g. chat.js button) that rely on globals.
window.sendCallInvite = sendCallInvite;

window.addEventListener('DOMContentLoaded', initCallManager);
