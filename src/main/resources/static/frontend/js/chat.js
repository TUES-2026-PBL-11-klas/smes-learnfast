// LearnFast — Chat (WebSocket + STOMP) — Fixed & Enhanced

let currentUser = null;
let chatUserId = null;
let stompClient = null;
let wsConnected = false;
let reconnectAttempts = 0;
const MAX_RECONNECT = 10;

(async function() {
    currentUser = await API.requireAuth();
    if (!currentUser) return;

    // Connect WebSocket
    connectWebSocket();

    // Load conversations
    await loadConversations();

    // Load group channels in the same sidebar
    await loadChannels();

    // Check if opening specific chat from URL
    const params = new URLSearchParams(window.location.search);
    const targetUser = params.get('user');
    if (targetUser) {
        openChat(parseInt(targetUser));
    }
})();

function connectWebSocket() {
    try {
        const socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);
        stompClient.debug = null; // Disable debug logs

        stompClient.connect({}, () => {
            wsConnected = true;
            reconnectAttempts = 0;

            // Subscribe to our personal channel
            stompClient.subscribe(`/topic/chat/${currentUser.id}`, (msg) => {
                const data = JSON.parse(msg.body);
                // If this message is for the current open chat, display it
                if (chatUserId &&
                    (data.senderId === chatUserId || data.receiverId === chatUserId)) {
                    appendMessage(data, true);
                }
                // Refresh conversation list
                loadConversations();
            });

            // Subscribe to typing indicators
            stompClient.subscribe(`/topic/typing/${currentUser.id}`, (msg) => {
                const data = JSON.parse(msg.body);
                if (chatUserId === data.senderId) {
                    showTypingIndicator(data.typing);
                }
            });
        }, (err) => {
            console.error('WebSocket error:', err);
            wsConnected = false;
            // Exponential backoff reconnect
            if (reconnectAttempts < MAX_RECONNECT) {
                const delay = Math.min(2000 * Math.pow(1.5, reconnectAttempts), 30000);
                reconnectAttempts++;
                setTimeout(connectWebSocket, delay);
            }
        });

        socket.onclose = () => {
            wsConnected = false;
            if (reconnectAttempts < MAX_RECONNECT) {
                const delay = Math.min(2000 * Math.pow(1.5, reconnectAttempts), 30000);
                reconnectAttempts++;
                setTimeout(connectWebSocket, delay);
            }
        };
    } catch(e) {
        console.error('WebSocket connection failed:', e);
        wsConnected = false;
    }
}

async function loadConversations() {
    const convos = await API.get('/api/chat/conversations');
    const list = document.getElementById('chat-list');

    if (!Array.isArray(convos) || convos.length === 0) {
        // If we have an active chat, still show that user
        if (chatUserId) {
            // Keep the list as-is or show the current partner
            return;
        }
        list.innerHTML = `
            <div class="empty-state" style="padding: 2rem;">
                <div class="empty-icon">💬</div>
                <p class="text-sm text-muted">No conversations yet</p>
                <a href="/frontend/pages/mentors.html" class="btn btn-primary btn-sm mt-2">Find Mentors</a>
            </div>
        `;
        return;
    }

    // Check if the current chat partner is in the conversations
    let hasCurrentPartner = false;
    if (chatUserId) {
        hasCurrentPartner = convos.some(c => c.id === chatUserId);
    }

    list.innerHTML = convos.map((c, idx) => {
        const isActive = chatUserId === c.id;
        if (isActive) hasCurrentPartner = true;
        return `
            <div class="chat-contact ${isActive ? 'active' : ''} chat-contact-animate" style="animation-delay: ${idx * 0.05}s" onclick="openChat(${c.id})">
                ${API.avatarHtml(c.name, c.avatarUrl)}
                <div class="chat-contact-info">
                    <div class="chat-contact-name">${c.name}</div>
                    <div class="chat-contact-preview">@${c.username}</div>
                </div>
            </div>
        `;
    }).join('');

    // Re-render the channels section because we just blew it away with innerHTML.
    if (typeof loadChannels === 'function') loadChannels();
}

async function openChat(userId) {
    chatUserId = userId;
    // Leaving any channel context so sendMessage routes to DMs again
    chatChannelId = null;
    if (channelWsSub) { try { channelWsSub.unsubscribe(); } catch (_) {} channelWsSub = null; }

    // Get user info
    let chatPartner;
    try {
        chatPartner = await API.get(`/api/mentors/${userId}`);
    } catch(e) {
        chatPartner = { name: 'User', username: 'user' };
    }

    // Update main area with animation
    const main = document.getElementById('chat-main');
    main.classList.add('chat-main-entering');
    const safeName = chatPartner.name || 'User';
    const safeUser = chatPartner.username || '';
    main.innerHTML = `
        <div class="chat-header chat-header-animate">
            ${API.avatarHtml(safeName, chatPartner.avatarUrl)}
            <div class="chat-header-info">
                <h4>${escapeHtml(safeName)}</h4>
                <p>@${escapeHtml(safeUser)}</p>
            </div>
            <div class="chat-header-actions">
                <button class="btn btn-primary btn-sm" id="chat-call-btn">📞 Call</button>
                <a href="/frontend/pages/profile.html?id=${userId}" class="btn btn-secondary btn-sm">Profile</a>
            </div>
        </div>
        <div class="chat-messages" id="chat-messages">
            <div class="page-loader"><div class="loader"></div></div>
        </div>
        <div class="chat-input-area chat-input-animate">
            <input type="file" id="file-input" style="display:none" accept="*/*">
            <button class="btn btn-secondary chat-attach-btn" id="attach-btn" title="Attach file">📎</button>
            <input type="text" class="form-control" id="chat-input" placeholder="Type a message..."
                   onkeypress="if(event.key==='Enter')sendMessage()"
                   oninput="handleTypingEvent()">
            <button class="btn btn-primary chat-send-btn" id="send-btn" onclick="sendMessage()">
                <span class="send-icon">➤</span>
            </button>
        </div>
    `;
    // Wire the call button after innerHTML so we don't have to escape user
    // input into an inline onclick attribute.
    const callBtn = document.getElementById('chat-call-btn');
    if (callBtn) {
        callBtn.addEventListener('click', () => _startDirectCall(userId, safeName));
    }

    const attachBtn = document.getElementById('attach-btn');
    const fileInput = document.getElementById('file-input');
    if (attachBtn && fileInput) {
        attachBtn.addEventListener('click', () => fileInput.click());
        fileInput.addEventListener('change', () => {
            if (fileInput.files[0]) _uploadAndSend(fileInput.files[0]);
            fileInput.value = '';
        });
    }

    requestAnimationFrame(() => {
        main.classList.remove('chat-main-entering');
    });

    // ── Session gate check ────────────────────────────────────────────────────
    const access = await API.get(`/api/chat/can-chat/${userId}`);
    if (!access.canChat) {
        const msgContainer = document.getElementById('chat-messages');
        msgContainer.innerHTML = '';

        if (access.reason === 'PENDING_SESSION') {
            msgContainer.innerHTML = `
                <div class="chat-locked">
                    <div style="font-size:2.5rem;">⏳</div>
                    <h4>Waiting for approval</h4>
                    <p>Session request is pending. Chat will unlock once the mentor accepts.</p>
                </div>`;
        } else if (access.canRequest) {
            // Student without any session — show Request button
            msgContainer.innerHTML = `
                <div class="chat-locked">
                    <div style="font-size:2.5rem;">🔒</div>
                    <h4>Session required</h4>
                    <p>Send a session request to this mentor. Once they accept, you can chat.</p>
                    <button class="btn btn-primary mt-2" id="req-session-btn">📅 Request Session</button>
                </div>`;
            document.getElementById('req-session-btn')
                .addEventListener('click', () => _requestSession(userId));
        } else {
            // Mentor side — student hasn't requested yet
            msgContainer.innerHTML = `
                <div class="chat-locked">
                    <div style="font-size:2.5rem;">🔒</div>
                    <h4>No session yet</h4>
                    <p>This student hasn't sent a session request to you yet.</p>
                </div>`;
        }

        // Hide the chat input bar
        const inputArea = document.querySelector('.chat-input-area');
        if (inputArea) inputArea.style.display = 'none';
        const callBtnEl = document.getElementById('chat-call-btn');
        if (callBtnEl) callBtnEl.style.display = 'none';

        requestAnimationFrame(() => main.classList.remove('chat-main-entering'));
        return;
    }

    // Load history
    const messages = await API.get(`/api/chat/history/${userId}`);
    const msgContainer = document.getElementById('chat-messages');
    msgContainer.innerHTML = '';

    if (Array.isArray(messages) && messages.length > 0) {
        messages.forEach((m, idx) => appendMessage(m, false, idx));
    } else {
        // Show "start a conversation" prompt
        msgContainer.innerHTML = `
            <div class="chat-start-prompt">
                <div class="chat-start-icon">👋</div>
                <p>Start a conversation with <strong>${chatPartner.name || 'this user'}</strong></p>
                <span class="text-muted text-sm">Send a message to get started</span>
            </div>
        `;
    }

    // Scroll to bottom
    msgContainer.scrollTop = msgContainer.scrollHeight;

    // Focus input
    document.getElementById('chat-input').focus();

    // Update sidebar active state
    loadConversations();

    // Ensure the current partner appears in sidebar even if no conversation yet
    ensurePartnerInSidebar(chatPartner);
}

function ensurePartnerInSidebar(partner) {
    const list = document.getElementById('chat-list');
    // Check if partner already in sidebar
    const existingContacts = list.querySelectorAll('.chat-contact');
    let found = false;
    existingContacts.forEach(c => {
        if (c.onclick && c.onclick.toString().includes(chatUserId)) {
            found = true;
        }
    });

    // Check by looking at the HTML for the partner's ID
    if (list.innerHTML.includes(`openChat(${chatUserId})`)) {
        found = true;
    }

    if (!found && partner && partner.name) {
        // Remove empty state if present
        const emptyState = list.querySelector('.empty-state');
        if (emptyState) emptyState.remove();

        const div = document.createElement('div');
        div.className = 'chat-contact active chat-contact-animate';
        div.onclick = () => openChat(chatUserId);
        div.innerHTML = `
            ${API.avatarHtml(partner.name, partner.avatarUrl)}
            <div class="chat-contact-info">
                <div class="chat-contact-name">${partner.name}</div>
                <div class="chat-contact-preview">@${partner.username || ''}</div>
            </div>
        `;
        list.prepend(div);
    }
}

function appendMessage(msg, animate = true, staggerIndex = 0) {
    const container = document.getElementById('chat-messages');
    if (!container) return;

    const prompt = container.querySelector('.chat-start-prompt');
    if (prompt) prompt.remove();

    if (msg.id && container.querySelector(`[data-msg-id="${msg.id}"]`)) return;

    const div = document.createElement('div');
    if (msg.id) div.setAttribute('data-msg-id', msg.id);

    if (msg.messageType === 'CALL_EVENT') {
        div.className = 'call-event-msg' + (animate ? ' msg-animate-in' : '');
        div.innerHTML = _renderCallEvent(msg);
        container.appendChild(div);
        container.scrollTop = container.scrollHeight;
        return;
    }

    const isSent = msg.senderId === currentUser.id;
    const time = new Date(msg.sentAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

    div.className = `chat-message ${isSent ? 'sent' : 'received'}`;
    if (animate) {
        div.classList.add('msg-animate-in');
    } else if (staggerIndex !== undefined) {
        div.classList.add('msg-stagger-in');
        div.style.animationDelay = `${Math.min(staggerIndex * 0.03, 1)}s`;
    }
    if (msg.messageType === 'IMAGE') {
        div.innerHTML = `
            <div class="msg-image"><img src="${msg.message}" alt="image" loading="lazy" onclick="window.open('${msg.message}','_blank')"></div>
            <div class="msg-time">${time}</div>
        `;
    } else if (msg.messageType === 'FILE') {
        const fname = decodeURIComponent(msg.message.split('/').pop());
        div.innerHTML = `
            <div class="msg-text msg-file"><a href="${msg.message}" download target="_blank">📎 ${escapeHtml(fname)}</a></div>
            <div class="msg-time">${time}</div>
        `;
    } else {
        div.innerHTML = `
            <div class="msg-text">${escapeHtml(msg.message)}</div>
            <div class="msg-time">${time}</div>
        `;
    }
    container.appendChild(div);
    container.scrollTop = container.scrollHeight;
}

function _renderCallEvent(msg) {
    const [event, secStr] = (msg.message || '').split(':');
    const secs = parseInt(secStr || '0', 10);
    const dur  = secs > 0
        ? ` · ${Math.floor(secs/60)}:${String(secs%60).padStart(2,'0')}`
        : '';
    const time = new Date(msg.sentAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    const outgoing = msg.senderId === currentUser.id;

    const map = {
        RINGING:  { icon: outgoing ? '📞' : '📲', label: outgoing ? 'Outgoing call' : 'Incoming call',  color: '#94a3b8' },
        ACCEPTED: { icon: '📞',  label: 'Call started',  color: '#22c55e' },
        ENDED:    { icon: '✅',  label: 'Call ended' + dur, color: '#6366f1' },
        DECLINED: { icon: '📵',  label: outgoing ? 'Call declined' : 'You declined', color: '#ef4444' },
        MISSED:   { icon: '📴',  label: outgoing ? 'No answer'    : 'Missed call',   color: '#f59e0b' },
    };
    const { icon, label, color } = map[event] || { icon: '📞', label: event, color: '#94a3b8' };

    return `<span style="color:${color};font-size:0.82rem;">${icon} ${label}</span>
            <span style="color:rgba(255,255,255,0.3);font-size:0.72rem;margin-left:0.5rem;">${time}</span>`;
}

async function sendMessage() {
    // Route to channel flow when a channel is open
    if (chatChannelId) { await _sendChannelMessage(); return; }

    const input = document.getElementById('chat-input');
    const text = input.value.trim();
    if (!text || !chatUserId) return;

    const sendBtn = document.getElementById('send-btn');
    input.value = '';
    input.focus();

    // Add sending animation to button
    sendBtn.classList.add('sending');

    try {
        // Send via REST (which also broadcasts via WebSocket)
        const result = await API.post('/api/chat/send', {
            receiverId: chatUserId,
            message: text
        });

        sendBtn.classList.remove('sending');

        // Directly append from REST response as fallback (duplicate check in appendMessage prevents doubles)
        if (result && !result.error) {
            appendMessage(result, true);
            // Refresh conversations to show this user in sidebar
            loadConversations();
        } else {
            API.showAlert(result?.error || 'Failed to send message', 'error');
        }
    } catch(e) {
        console.error('Send failed:', e);
        sendBtn.classList.remove('sending');
        API.showAlert('Failed to send message', 'error');
    }
}

async function _uploadAndSend(file) {
    if (!chatUserId && !chatChannelId) return;
    const attachBtn = document.getElementById('attach-btn');
    if (attachBtn) { attachBtn.disabled = true; attachBtn.textContent = '⏳'; }

    try {
        const fd = new FormData();
        fd.append('file', file);
        const res = await fetch('/api/upload', { method: 'POST', credentials: 'include', body: fd });
        const data = await res.json();
        if (data.error) { API.showAlert(data.error, 'error'); return; }

        if (chatChannelId) {
            // Channel flow
            const result = await API.post(`/api/channels/${chatChannelId}/messages`, {
                message: data.url,
                messageType: data.messageType
            });
            if (result && !result.error) {
                appendChannelMessage(result, true);
            } else {
                API.showAlert(result?.error || 'Failed to send file', 'error');
            }
        } else {
            // Direct-message flow
            const result = await API.post('/api/chat/send', {
                receiverId: chatUserId,
                message: data.url,
                messageType: data.messageType
            });
            if (result && !result.error) {
                appendMessage(result, true);
                loadConversations();
            } else {
                API.showAlert(result?.error || 'Failed to send file', 'error');
            }
        }
    } catch(e) {
        console.error('Upload failed:', e);
        API.showAlert('Upload failed', 'error');
    } finally {
        if (attachBtn) { attachBtn.disabled = false; attachBtn.textContent = '📎'; }
    }
}

let typingTimeout = null;
let isCurrentlyTyping = false;

function handleTypingEvent() {
    if (!chatUserId || !wsConnected) return;

    if (!isCurrentlyTyping) {
        isCurrentlyTyping = true;
        sendTypingStatus(true);
    }

    if (typingTimeout) clearTimeout(typingTimeout);

    typingTimeout = setTimeout(() => {
        isCurrentlyTyping = false;
        sendTypingStatus(false);
    }, 2000);
}

function sendTypingStatus(typing) {
    if (stompClient && wsConnected) {
        stompClient.send("/app/chat.typing", {}, JSON.stringify({
            senderId: currentUser.id,
            receiverId: chatUserId,
            typing: typing
        }));
    }
}

function showTypingIndicator(show) {
    const container = document.getElementById('chat-messages');
    if (!container) return;

    let indicator = document.getElementById('typing-indicator');

    if (show) {
        if (!indicator) {
            indicator = document.createElement('div');
            indicator.id = 'typing-indicator';
            indicator.className = 'chat-message received typing-indicator msg-animate-in';
            indicator.innerHTML = `
                <div class="typing-dots">
                    <span></span><span></span><span></span>
                </div>
            `;
            container.appendChild(indicator);
            container.scrollTop = container.scrollHeight;
        }
    } else {
        if (indicator) indicator.remove();
    }
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// ── Request a session from within chat ────────────────────────────────────────
async function _requestSession(mentorId) {
    const btn = document.querySelector('.chat-locked .btn-primary');
    if (btn) { btn.disabled = true; btn.textContent = 'Sending…'; }
    const res = await API.post('/api/sessions', { mentorId });
    if (res && !res.error) {
        const box = document.querySelector('.chat-locked');
        if (box) box.innerHTML = `
            <div style="font-size:2.5rem;">⏳</div>
            <h4>Request sent!</h4>
            <p>Waiting for the mentor to accept your session request.</p>`;
    } else {
        if (btn) { btn.disabled = false; btn.textContent = '📅 Request Session'; }
        API.showAlert(res?.error || 'Could not send request', 'error');
    }
}

// ── Direct call from chat (roomId is deterministic from both user IDs) ─────────
function _startDirectCall(targetUserId, targetName) {
    if (typeof sendCallInvite === 'function') {
        const a = Math.min(currentUser.id, targetUserId);
        const b = Math.max(currentUser.id, targetUserId);
        const roomId = `dm-${a}-${b}`;
        sendCallInvite(roomId, targetUserId, targetName);
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Channels (group chat + group call)
// ─────────────────────────────────────────────────────────────────────────────
let chatChannelId = null;
let chatChannelName = '';
let channelWsSub = null;

async function loadChannels() {
    const channels = await API.get('/api/channels');
    const list = document.getElementById('chat-list');
    if (!list) return;

    const isMentor = currentUser && currentUser.role === 'mentor';

    // Build section header + optional "new channel" button
    let header = '<div style="padding:0.75rem 1rem 0.25rem;display:flex;align-items:center;justify-content:space-between;">'
        + '<span style="font-size:0.72rem;font-weight:600;color:rgba(255,255,255,0.35);text-transform:uppercase;letter-spacing:0.08em;">Channels</span>';
    if (isMentor) {
        header += '<button class="btn btn-secondary btn-sm" onclick="createChannelPrompt()" '
            + 'style="padding:0.15rem 0.5rem;font-size:0.75rem;">＋ New</button>';
    }
    header += '</div>';

    let body = '';
    if (Array.isArray(channels) && channels.length > 0) {
        body = channels.map(c => `
            <div class="chat-contact" onclick='openChannel(${c.id}, ${JSON.stringify(c.name)})'>
                <div class="avatar avatar-sm" style="background:linear-gradient(135deg,#5865f2,#7c6fff);font-size:1rem;">#</div>
                <div class="chat-contact-info">
                    <div class="chat-contact-name">${escapeHtml(c.name)}</div>
                    <div class="chat-contact-preview">${c.members.length} member${c.members.length !== 1 ? 's' : ''}</div>
                </div>
            </div>
        `).join('');
    } else if (!isMentor) {
        // Students with no channels: render nothing at all
        return;
    }

    // Remove previous channels section if re-rendering, then append fresh
    const existing = list.querySelector('[data-channels-section]');
    if (existing) existing.remove();

    const wrap = document.createElement('div');
    wrap.setAttribute('data-channels-section', '1');
    wrap.innerHTML = header + body;
    list.appendChild(wrap);
}

async function openChannel(id, name) {
    chatChannelId = id;
    chatChannelName = name;
    chatUserId = null;

    // Drop any previous channel subscription
    if (channelWsSub) { try { channelWsSub.unsubscribe(); } catch (_) {} channelWsSub = null; }

    const channelData = await API.get(`/api/channels/${id}`);
    if (!channelData || channelData.error) {
        API.showAlert(channelData?.error || 'Could not open channel', 'error');
        return;
    }
    const isCreator = channelData.creatorId === currentUser.id;

    const main = document.getElementById('chat-main');
    const membersHtml = (channelData.members || [])
        .map(m => API.avatarHtml(m.name, m.avatarUrl))
        .join('');

    const safeName = name || 'Channel';
    main.innerHTML = `
        <div class="chat-header chat-header-animate">
            <div class="avatar avatar-sm" style="background:linear-gradient(135deg,#5865f2,#7c6fff);font-size:1rem;flex-shrink:0;">#</div>
            <div class="chat-header-info">
                <h4>${escapeHtml(safeName)}</h4>
                <div style="display:flex;gap:0.25rem;margin-top:0.2rem;flex-wrap:wrap;">${membersHtml}</div>
            </div>
            <div class="chat-header-actions">
                ${isCreator ? `<button class="btn btn-secondary btn-sm" id="ch-add-btn">➕ Add</button>` : ''}
                <button class="btn btn-primary btn-sm" id="ch-call-btn">📞 Call</button>
            </div>
        </div>
        <div class="chat-messages" id="chat-messages">
            <div class="page-loader"><div class="loader"></div></div>
        </div>
        <div class="chat-input-area chat-input-animate">
            <input type="file" id="file-input" style="display:none" accept="*/*">
            <button class="btn btn-secondary chat-attach-btn" id="attach-btn" title="Attach file">📎</button>
            <input type="text" class="form-control" id="chat-input" placeholder="Message #${escapeHtml(safeName)}…"
                   onkeypress="if(event.key==='Enter')sendMessage()">
            <button class="btn btn-primary chat-send-btn" id="send-btn" onclick="sendMessage()">
                <span class="send-icon">➤</span>
            </button>
        </div>
    `;

    // Wire up header buttons (avoid escaping channel name into onclick attrs)
    const addBtn = document.getElementById('ch-add-btn');
    if (addBtn) addBtn.addEventListener('click', () => openAddMemberModal(id));
    const callBtn = document.getElementById('ch-call-btn');
    if (callBtn) callBtn.addEventListener('click', () => _startGroupCall(id, safeName));

    // Wire file upload
    const attachBtn = document.getElementById('attach-btn');
    const fileInput = document.getElementById('file-input');
    if (attachBtn && fileInput) {
        attachBtn.addEventListener('click', () => fileInput.click());
        fileInput.addEventListener('change', () => {
            if (fileInput.files[0]) _uploadAndSend(fileInput.files[0]);
            fileInput.value = '';
        });
    }

    // Load message history
    const msgs = await API.get(`/api/channels/${id}/messages`);
    const container = document.getElementById('chat-messages');
    if (container) {
        container.innerHTML = '';
        if (Array.isArray(msgs) && msgs.length > 0) {
            msgs.forEach((m, i) => appendChannelMessage(m, false, i));
        } else {
            container.innerHTML = '<div class="chat-start-prompt" style="text-align:center;color:rgba(255,255,255,0.3);padding:2rem;">No messages yet. Start the conversation!</div>';
        }
        container.scrollTop = container.scrollHeight;
    }

    // Subscribe for real-time channel messages (reuse shared stompClient)
    if (typeof stompClient !== 'undefined' && stompClient && stompClient.connected) {
        channelWsSub = stompClient.subscribe(`/topic/channel/${id}`, (frame) => {
            try {
                const msg = JSON.parse(frame.body);
                appendChannelMessage(msg, true);
            } catch (e) { console.warn('Bad channel frame', e); }
        });
    }

    const input = document.getElementById('chat-input');
    if (input) input.focus();
}

function appendChannelMessage(msg, animate = true, staggerIndex = 0) {
    const container = document.getElementById('chat-messages');
    if (!container) return;
    // Remove empty-state prompt if present
    const prompt = container.querySelector('.chat-start-prompt');
    if (prompt) prompt.remove();
    if (msg.id && container.querySelector(`[data-msg-id="${msg.id}"]`)) return;

    const isSent = msg.senderId === currentUser.id;
    const time = new Date(msg.sentAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

    const div = document.createElement('div');
    if (msg.id) div.setAttribute('data-msg-id', msg.id);
    div.className = `chat-message ${isSent ? 'sent' : 'received'}`;
    if (animate) div.classList.add('msg-animate-in');
    else {
        div.classList.add('msg-stagger-in');
        div.style.animationDelay = `${Math.min(staggerIndex * 0.03, 1)}s`;
    }

    const senderLabel = isSent ? ''
        : `<div style="font-size:0.72rem;color:rgba(255,255,255,0.45);margin-bottom:0.15rem;">${escapeHtml(msg.senderName || '')}</div>`;

    if (msg.messageType === 'IMAGE') {
        div.innerHTML = `${senderLabel}<div class="msg-image"><img src="${msg.message}" alt="image" loading="lazy" onclick="window.open('${msg.message}','_blank')"></div><div class="msg-time">${time}</div>`;
    } else if (msg.messageType === 'FILE') {
        const fname = decodeURIComponent(msg.message.split('/').pop());
        div.innerHTML = `${senderLabel}<div class="msg-text msg-file"><a href="${msg.message}" download target="_blank">📎 ${escapeHtml(fname)}</a></div><div class="msg-time">${time}</div>`;
    } else {
        div.innerHTML = `${senderLabel}<div class="msg-text">${escapeHtml(msg.message)}</div><div class="msg-time">${time}</div>`;
    }
    container.appendChild(div);
    container.scrollTop = container.scrollHeight;
}

async function _sendChannelMessage() {
    const input = document.getElementById('chat-input');
    if (!input) return;
    const text = input.value.trim();
    if (!text) return;
    input.value = '';
    input.focus();
    const result = await API.post(`/api/channels/${chatChannelId}/messages`, {
        message: text,
        messageType: 'TEXT'
    });
    if (result && !result.error) {
        appendChannelMessage(result, true);
    } else {
        API.showAlert(result?.error || 'Failed to send', 'error');
    }
}

async function _startGroupCall(channelId, name) {
    const res = await API.post(`/api/channels/${channelId}/call/start`, {});
    if (res && res.error) { API.showAlert(res.error, 'error'); return; }
    window.location.href =
        `/frontend/pages/group-call.html?channelId=${channelId}` +
        `&channelName=${encodeURIComponent(name)}`;
}

async function openAddMemberModal(channelId) {
    // Fetch all users to show a picker
    const [allUsers, channelData, sessions] = await Promise.all([
        API.get('/api/users'),
        API.get(`/api/channels/${channelId}`),
        API.get('/api/sessions')
    ]);

    const existingIds = new Set([
        channelData.creatorId,
        ...(channelData.members || []).map(m => m.id)
    ]);

    // Only students with an ACCEPTED session with this mentor
    const acceptedStudentIds = new Set(
        Array.isArray(sessions)
            ? sessions
                .filter(s => s.status === 'ACCEPTED')
                .map(s => s.student?.id)
                .filter(Boolean)
            : []
    );

    const candidates = Array.isArray(allUsers)
        ? allUsers.filter(u =>
            u.role === 'student' &&
            acceptedStudentIds.has(u.id) &&
            !existingIds.has(u.id)
          )
        : [];

    // Remove any existing modal
    document.getElementById('__add-member-modal')?.remove();

    const overlay = document.createElement('div');
    overlay.id = '__add-member-modal';
    overlay.style.cssText = 'position:fixed;inset:0;background:rgba(0,0,0,0.6);z-index:99999;display:flex;align-items:center;justify-content:center;';

    const box = document.createElement('div');
    box.style.cssText = 'background:#1e1f22;border-radius:14px;padding:1.5rem;width:340px;max-height:80vh;display:flex;flex-direction:column;gap:1rem;border:1px solid rgba(255,255,255,0.1);';

    const title = document.createElement('h4');
    title.textContent = 'Add member';
    title.style.cssText = 'margin:0;font-size:1rem;color:#fff;';

    const search = document.createElement('input');
    search.type = 'text';
    search.placeholder = 'Search by name or username…';
    search.className = 'form-control';
    search.style.cssText = 'font-size:0.88rem;';

    const list = document.createElement('div');
    list.style.cssText = 'overflow-y:auto;flex:1;display:flex;flex-direction:column;gap:0.4rem;max-height:300px;';

    function renderList(filter) {
        list.innerHTML = '';
        const filtered = candidates.filter(u =>
            !filter || u.name.toLowerCase().includes(filter) || u.username.toLowerCase().includes(filter)
        );
        if (filtered.length === 0) {
            list.innerHTML = '<p style="color:rgba(255,255,255,0.4);font-size:0.85rem;text-align:center;padding:1rem 0;">No users found</p>';
            return;
        }
        filtered.forEach(u => {
            const row = document.createElement('div');
            row.style.cssText = 'display:flex;align-items:center;gap:0.75rem;padding:0.5rem 0.6rem;border-radius:8px;cursor:pointer;transition:background 0.15s;';
            row.onmouseenter = () => row.style.background = 'rgba(255,255,255,0.07)';
            row.onmouseleave = () => row.style.background = '';
            row.innerHTML = `
                ${API.avatarHtml(u.name, u.avatarUrl)}
                <div style="flex:1;min-width:0;">
                    <div style="font-size:0.88rem;font-weight:600;color:#fff;">${escapeHtml(u.name)}</div>
                    <div style="font-size:0.75rem;color:rgba(255,255,255,0.45);">@${escapeHtml(u.username)} · ${u.role}</div>
                </div>
                <button style="background:#5865f2;color:#fff;border:none;border-radius:6px;padding:0.3rem 0.75rem;cursor:pointer;font-size:0.8rem;font-weight:600;">Add</button>
            `;
            row.querySelector('button').addEventListener('click', async () => {
                const result = await API.post(`/api/channels/${channelId}/members`, { userId: u.id });
                if (result && result.error) { API.showAlert(result.error, 'error'); return; }
                overlay.remove();
                API.showAlert(`${u.name} added!`, 'success');
                openChannel(channelId, chatChannelName);
                loadChannels();
            });
            list.appendChild(row);
        });
    }

    renderList('');
    search.addEventListener('input', () => renderList(search.value.toLowerCase().trim()));

    const closeBtn = document.createElement('button');
    closeBtn.textContent = 'Cancel';
    closeBtn.className = 'btn btn-secondary btn-sm';
    closeBtn.addEventListener('click', () => overlay.remove());
    overlay.addEventListener('click', e => { if (e.target === overlay) overlay.remove(); });

    box.append(title, search, list, closeBtn);
    overlay.appendChild(box);
    document.body.appendChild(overlay);
    search.focus();
}

async function createChannelPrompt() {
    const name = prompt('Channel name:');
    if (!name || !name.trim()) return;
    const result = await API.post('/api/channels', { name: name.trim() });
    if (result && result.error) { API.showAlert(result.error, 'error'); return; }
    API.showAlert('Channel created!', 'success');
    await loadChannels();
}
