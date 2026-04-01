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
        const initials = c.name.split(' ').map(w => w[0]).join('').toUpperCase();
        const isActive = chatUserId === c.id;
        if (isActive) hasCurrentPartner = true;
        return `
            <div class="chat-contact ${isActive ? 'active' : ''} chat-contact-animate" style="animation-delay: ${idx * 0.05}s" onclick="openChat(${c.id})">
                <div class="avatar avatar-sm">${initials}</div>
                <div class="chat-contact-info">
                    <div class="chat-contact-name">${c.name}</div>
                    <div class="chat-contact-preview">@${c.username}</div>
                </div>
            </div>
        `;
    }).join('');
}

async function openChat(userId) {
    chatUserId = userId;

    // Get user info
    let chatPartner;
    try {
        chatPartner = await API.get(`/api/mentors/${userId}`);
    } catch(e) {
        chatPartner = { name: 'User', username: 'user' };
    }

    const initials = chatPartner.name ? chatPartner.name.split(' ').map(w => w[0]).join('').toUpperCase() : '?';

    // Update main area with animation
    const main = document.getElementById('chat-main');
    main.classList.add('chat-main-entering');
    main.innerHTML = `
        <div class="chat-header chat-header-animate">
            <div class="avatar avatar-sm">${initials}</div>
            <div class="chat-header-info">
                <h4>${chatPartner.name || 'User'}</h4>
                <p>@${chatPartner.username || ''}</p>
            </div>
            <div class="chat-header-actions">
                <a href="/frontend/pages/profile.html?id=${userId}" class="btn btn-secondary btn-sm">Profile</a>
            </div>
        </div>
        <div class="chat-messages" id="chat-messages">
            <div class="page-loader"><div class="loader"></div></div>
        </div>
        <div class="chat-input-area chat-input-animate">
            <input type="text" class="form-control" id="chat-input" placeholder="Type a message..."
                   onkeypress="if(event.key==='Enter')sendMessage()"
                   oninput="handleTypingEvent()">
            <button class="btn btn-primary chat-send-btn" id="send-btn" onclick="sendMessage()">
                <span class="send-icon">➤</span>
            </button>
        </div>
    `;

    requestAnimationFrame(() => {
        main.classList.remove('chat-main-entering');
    });

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
        const initials = partner.name.split(' ').map(w => w[0]).join('').toUpperCase();
        // Remove empty state if present
        const emptyState = list.querySelector('.empty-state');
        if (emptyState) emptyState.remove();

        const div = document.createElement('div');
        div.className = 'chat-contact active chat-contact-animate';
        div.onclick = () => openChat(chatUserId);
        div.innerHTML = `
            <div class="avatar avatar-sm">${initials}</div>
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

    // Remove start prompt if present
    const prompt = container.querySelector('.chat-start-prompt');
    if (prompt) prompt.remove();

    const isSent = msg.senderId === currentUser.id;
    const time = new Date(msg.sentAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

    // Avoid duplicates by checking if message ID already exists
    if (msg.id && container.querySelector(`[data-msg-id="${msg.id}"]`)) return;

    const div = document.createElement('div');
    div.className = `chat-message ${isSent ? 'sent' : 'received'}`;
    if (animate) {
        div.classList.add('msg-animate-in');
    } else if (staggerIndex !== undefined) {
        div.classList.add('msg-stagger-in');
        div.style.animationDelay = `${Math.min(staggerIndex * 0.03, 1)}s`;
    }
    if (msg.id) div.setAttribute('data-msg-id', msg.id);
    div.innerHTML = `
        <div class="msg-text">${escapeHtml(msg.message)}</div>
        <div class="msg-time">${time}</div>
    `;
    container.appendChild(div);
    container.scrollTop = container.scrollHeight;
}

async function sendMessage() {
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
