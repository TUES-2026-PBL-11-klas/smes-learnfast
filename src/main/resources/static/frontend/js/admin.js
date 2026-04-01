// LearnFast — Admin Panel

let currentTab = 'subjects';

(async function() {
    const user = await API.requireAuth();
    if (!user) return;

    if (user.role !== 'admin') {
        document.querySelector('.admin-content').innerHTML = `
            <div class="empty-state" style="min-height: 60vh;">
                <div class="empty-icon">🔒</div>
                <h3>Access Denied</h3>
                <p class="text-muted">You need admin privileges to access this page.</p>
                <a href="/frontend/pages/dashboard.html" class="btn btn-primary mt-2">Go to Dashboard</a>
            </div>
        `;
        return;
    }

    loadSubjects();
})();

function showTab(tab) {
    currentTab = tab;
    document.querySelectorAll('.admin-nav-item').forEach(el => el.classList.remove('active'));
    document.getElementById(`tab-${tab}`).classList.add('active');

    document.getElementById('panel-subjects').style.display = tab === 'subjects' ? 'block' : 'none';
    document.getElementById('panel-users').style.display = tab === 'users' ? 'block' : 'none';

    if (tab === 'subjects') loadSubjects();
    if (tab === 'users') loadUsers();
}

// === Subjects ===
async function loadSubjects() {
    const subjects = await API.get('/api/admin/subjects');
    const tbody = document.getElementById('subjects-table-body');

    if (!Array.isArray(subjects) || subjects.length === 0) {
        tbody.innerHTML = '<tr><td colspan="3" class="text-center text-muted" style="padding:2rem;">No subjects yet. Add one above!</td></tr>';
        return;
    }

    tbody.innerHTML = subjects.map(s => `
        <tr>
            <td>${s.id}</td>
            <td><strong>${s.name}</strong></td>
            <td>
                <button class="btn btn-danger btn-sm" onclick="deleteSubject(${s.id})">Delete</button>
            </td>
        </tr>
    `).join('');
}

async function addSubject() {
    const input = document.getElementById('new-subject-name');
    const name = input.value.trim();
    if (!name) return;

    const res = await API.post('/api/admin/subjects', { name });
    if (res.error) {
        API.showAlert(res.error, 'error');
        return;
    }

    input.value = '';
    API.showAlert(`Subject "${name}" added!`, 'success');
    loadSubjects();
}

async function deleteSubject(id) {
    if (!confirm('Are you sure you want to delete this subject?')) return;
    await API.delete(`/api/admin/subjects/${id}`);
    loadSubjects();
}

// === Users ===
async function loadUsers() {
    const users = await API.get('/api/admin/users');
    const tbody = document.getElementById('users-table-body');

    if (!Array.isArray(users) || users.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted" style="padding:2rem;">No users found</td></tr>';
        return;
    }

    tbody.innerHTML = users.map(u => {
        const roleBadge = {
            admin: 'badge-purple',
            mentor: 'badge-blue',
            student: 'badge-green'
        }[u.role] || 'badge-blue';

        return `
            <tr>
                <td>${u.id}</td>
                <td><strong>${u.name}</strong></td>
                <td>@${u.username}</td>
                <td>${u.email}</td>
                <td><span class="badge ${roleBadge}">${u.role}</span></td>
                <td>
                    ${u.role !== 'admin' ? `<button class="btn btn-danger btn-sm" onclick="deleteUser(${u.id})">Delete</button>` : '<span class="text-muted text-sm">Protected</span>'}
                </td>
            </tr>
        `;
    }).join('');
}

async function deleteUser(id) {
    if (!confirm('Are you sure you want to delete this user? This cannot be undone.')) return;
    await API.delete(`/api/admin/users/${id}`);
    loadUsers();
}
