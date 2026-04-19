// LearnFast — Dashboard

(async function() {
    const user = await API.requireAuth();
    if (!user) return;

    // Greeting
    const hour = new Date().getHours();
    let greeting = hour < 12 ? 'Good morning' : hour < 18 ? 'Good afternoon' : 'Good evening';
    document.getElementById('greeting').textContent = `${greeting}, ${user.name}! 👋`;

    // Show quick actions for students
    if (user.role === 'student') {
        document.getElementById('quick-actions').style.display = 'block';
    }

    // Load sessions
    try {
        const sessions = await API.get('/api/sessions');
        if (Array.isArray(sessions)) {
            document.getElementById('stat-sessions').textContent = sessions.length;
            document.getElementById('stat-pending').textContent =
                sessions.filter(s => s.status === 'PENDING').length;

            const container = document.getElementById('recent-sessions');
            if (sessions.length > 0) {
                container.innerHTML = sessions.slice(0, 5).map(s => {
                    const other = user.role === 'student' ? s.mentor : s.student;
                    const initials = other.name.split(' ').map(w => w[0]).join('').toUpperCase();
                    const statusClass = {
                        PENDING: 'badge-orange',
                        ACCEPTED: 'badge-green',
                        REJECTED: 'badge-red',
                        COMPLETED: 'badge-blue'
                    }[s.status] || 'badge-blue';

                    return `
                        <div class="session-item">
                            <div class="avatar avatar-sm">${initials}</div>
                            <div class="session-info">
                                <h4>${other.name}</h4>
                                <p>${new Date(s.createdAt).toLocaleDateString()}</p>
                            </div>
                            <span class="badge ${statusClass}">${s.status}</span>
                            ${s.status === 'PENDING' && user.role === 'mentor' ? `
                                <button class="btn btn-success btn-sm" onclick="event.stopPropagation(); acceptSession(${s.id})">✓</button>
                                <button class="btn btn-danger btn-sm" onclick="event.stopPropagation(); rejectSession(${s.id})">✗</button>
                            ` : ''}
                        </div>
                    `;
                }).join('');
            }
        }
    } catch (e) { console.error('Sessions error:', e); }

    // Load call history
    try {
        const calls = await API.get('/api/calls');
        if (Array.isArray(calls) && calls.length > 0) {
            const container = document.getElementById('call-history');
            container.innerHTML = calls.slice(0, 10).map(c => {
                const icons = { ACCEPTED: '📞', DECLINED: '📵', MISSED: '📴', ENDED: '✅', RINGING: '⏳' };
                const colors = { ACCEPTED: '#22c55e', DECLINED: '#ef4444', MISSED: '#f59e0b', ENDED: '#6366f1', RINGING: '#94a3b8' };
                const labels = { ACCEPTED: 'Accepted', DECLINED: 'Declined', MISSED: 'Missed', ENDED: 'Ended', RINGING: 'Ringing' };
                const icon  = icons[c.status]  || '📞';
                const color = colors[c.status] || '#94a3b8';
                const label = labels[c.status] || c.status;
                const dir   = c.outgoing ? '↗ Outgoing' : '↙ Incoming';
                const dur   = c.durationSeconds > 0
                    ? `${Math.floor(c.durationSeconds/60)}:${String(c.durationSeconds%60).padStart(2,'0')}`
                    : '';
                const time  = new Date(c.startedAt).toLocaleString([], { month:'short', day:'numeric', hour:'2-digit', minute:'2-digit' });
                return `
                    <div class="session-item" style="gap:0.75rem;">
                        <div style="font-size:1.4rem;min-width:32px;text-align:center;">${icon}</div>
                        <div class="session-info">
                            <h4>${c.otherName}</h4>
                            <p style="color:rgba(255,255,255,0.4);font-size:0.78rem;">${dir} · ${time}</p>
                        </div>
                        <div style="text-align:right;margin-left:auto;">
                            <span style="color:${color};font-size:0.82rem;font-weight:600;">${label}</span>
                            ${dur ? `<div style="color:rgba(255,255,255,0.35);font-size:0.75rem;">${dur}</div>` : ''}
                        </div>
                    </div>`;
            }).join('');
        }
    } catch (e) { console.error('Call history error:', e); }

    // Load conversations
    try {
        const convos = await API.get('/api/chat/conversations');
        if (Array.isArray(convos)) {
            document.getElementById('stat-conversations').textContent = convos.length;

            const container = document.getElementById('recent-messages');
            if (convos.length > 0) {
                container.innerHTML = convos.slice(0, 5).map(c => {
                    const initials = c.name.split(' ').map(w => w[0]).join('').toUpperCase();
                    return `
                        <div class="message-item" onclick="window.location.href='/frontend/pages/chat.html?user=${c.id}'">
                            <div class="avatar avatar-sm">${initials}</div>
                            <div class="message-info">
                                <h4>${c.name}</h4>
                                <p>@${c.username}</p>
                            </div>
                        </div>
                    `;
                }).join('');
            }
        }
    } catch (e) { console.error('Conversations error:', e); }
})();

async function acceptSession(id) {
    await API.put(`/api/sessions/${id}/accept`);
    location.reload();
}

async function rejectSession(id) {
    await API.put(`/api/sessions/${id}/reject`);
    location.reload();
}
