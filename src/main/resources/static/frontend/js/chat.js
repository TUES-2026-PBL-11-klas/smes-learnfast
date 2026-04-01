// LearnFast — Chat (WebSocket + STOMP)

let currentUser = null;
let chatUserId = null;
let stompClient = null;

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
            // Subscribe to our personal channel
            stompClient.subscribe(`/topic/chat/${currentUser.id}`, (msg) => {
                const data = JSON.parse(msg.body);
                // If this message is for the current open chat, display it
                if (chatUserId &&
                    (data.senderId === chatUserId || data.receiverId === chatUserId)) {
                    appendMessage(data);
                }
                // Refresh conversation list
                loadConversations();
            });
        }, (err) => {
            console.error('WebSocket error:', err);
            // Fallback: poll for messages
            setTimeout(connectWebSocket, 5000);
        });
    } catch(e) {
        console.error('WebSocket connection failed:', e);
    }
}

async function loadConversations() {
    const convos = await API.get('/api/chat/conversations');
    const list = document.getElementById('chat-list');

    if (!Array.isArray(convos) || convos.length === 0) {
        list.innerHTML = `
            <div class="empty-state" style="padding: 2rem;">
                <div class="empty-icon">💬</div>
                <p class="text-sm text-muted">No conversations yet</p>
                <a href="/frontend/pages/mentors.html" class="btn btn-primary btn-sm mt-2">Find Mentors</a>
            </div>
        `;
        return;
    }

    list.innerHTML = convos.map(c => {
        const initials = c.name.split(' ').map(w => w[0]).join('').toUpperCase();
        const isActive = chatUserId === c.id;
        return `
            <div class="chat-contact ${isActive ? 'active' : ''}" onclick="openChat(${c.id})">
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

    // Update main area
    const main = document.getElementById('chat-main');
    main.innerHTML = `
        <div class="chat-header">
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
        <div class="chat-input-area">
            <input type="text" class="form-control" id="chat-input" placeholder="Type a message..."
                   onkeypress="if(event.key==='Enter')sendMessage()">
            <button class="btn btn-primary" onclick="sendMessage()">Send</button>
        </div>
    `;

    // Load history
    const messages = await API.get(`/api/chat/history/${userId}`);
    const msgContainer = document.getElementById('chat-messages');
    msgContainer.innerHTML = '';

    if (Array.isArray(messages)) {
        messages.forEach(m => appendMessage(m));
    }

    // Scroll to bottom
    msgContainer.scrollTop = msgContainer.scrollHeight;

    // Focus input
    document.getElementById('chat-input').focus();

    // Update sidebar active state
    loadConversations();
}

function appendMessage(msg) {
    const container = document.getElementById('chat-messages');
    if (!container) return;

    const isSent = msg.senderId === currentUser.id;
    const time = new Date(msg.sentAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

    // Avoid duplicates by checking if message ID already exists
    if (msg.id && container.querySelector(`[data-msg-id="${msg.id}"]`)) return;

    const div = document.createElement('div');
    div.className = `chat-message ${isSent ? 'sent' : 'received'}`;
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

    input.value = '';
    input.focus();

    try {
        // Send via REST (which also broadcasts via WebSocket)
        await API.post('/api/chat/send', {
            receiverId: chatUserId,
            message: text
        });
    } catch(e) {
        console.error('Send failed:', e);
        API.showAlert('Failed to send message', 'error');
    }
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}
