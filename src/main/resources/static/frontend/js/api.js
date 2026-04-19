// LearnFast — API Helper
const API = {
    async get(url) {
        const res = await fetch(url, { credentials: 'include' });
        return res.json();
    },

    async post(url, data) {
        const res = await fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify(data)
        });
        return res.json();
    },

    async put(url, data) {
        const res = await fetch(url, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify(data)
        });
        return res.json();
    },

    async delete(url) {
        const res = await fetch(url, {
            method: 'DELETE',
            credentials: 'include'
        });
        return res.json();
    },

    // Update navbar based on logged-in user
    updateNav(user) {
        const nav = document.getElementById('nav-links');
        if (!nav) return;

        if (user && !user.error) {
            nav.innerHTML = `
                <a href="/frontend/pages/dashboard.html">Dashboard</a>
                <a href="/frontend/pages/mentors.html">Mentors</a>
                <a href="/frontend/pages/chat.html">Chat</a>
                ${user.role === 'admin' ? '<a href="/frontend/pages/admin.html">Admin</a>' : ''}
                <span style="color:var(--text-muted);font-size:0.85rem;padding:0 0.5rem;">Hi, ${user.name.split(' ')[0]}</span>
                <a href="#" class="btn btn-secondary btn-sm" onclick="API.logout()">Logout</a>
            `;
        } else {
            nav.innerHTML = `
                <a href="/frontend/pages/login.html" class="btn btn-secondary btn-sm">Log In</a>
                <a href="/frontend/pages/register.html" class="btn btn-primary btn-sm">Sign Up</a>
            `;
        }
    },

    async logout() {
        await fetch('/api/auth/logout', { method: 'POST', credentials: 'include' });
        window.location.href = '/frontend/pages/index.html';
    },

    // Check auth and redirect if not logged in
    async requireAuth() {
        try {
            const user = await this.get('/api/auth/me');
            if (user.error) {
                window.location.href = '/frontend/pages/login.html';
                return null;
            }
            this.updateNav(user);
            return user;
        } catch (e) {
            window.location.href = '/frontend/pages/login.html';
            return null;
        }
    },

    // Show alert
    showAlert(message, type = 'error') {
        const container = document.getElementById('alert-container');
        if (!container) return;
        const icons = { error: '⚠️', success: '✅', info: 'ℹ️' };
        container.innerHTML = `
            <div class="alert alert-${type}">
                ${icons[type] || ''} ${message}
            </div>
        `;
        setTimeout(() => { container.innerHTML = ''; }, 5000);
    },

    // Returns avatar HTML: <img> if avatarUrl set, otherwise initials <div>
    avatarHtml(name, avatarUrl, sizeClass = 'avatar-sm') {
        const initials = (name || '?').split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 2);
        if (avatarUrl && avatarUrl.trim()) {
            return `<img src="${avatarUrl}" alt="${initials}" class="avatar ${sizeClass}"
                        style="object-fit:cover;border-radius:50%;"
                        onerror="this.outerHTML='<div class=\\'avatar ${sizeClass}\\'>${initials}</div>'">`;
        }
        return `<div class="avatar ${sizeClass}">${initials}</div>`;
    }
};
