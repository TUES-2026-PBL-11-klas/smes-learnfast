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
    loadPendingBadge();
})();

function showTab(tab) {
    currentTab = tab;
    document.querySelectorAll('.admin-nav-item').forEach(el => el.classList.remove('active'));
    document.getElementById(`tab-${tab}`).classList.add('active');

    document.getElementById('panel-subjects').style.display = tab === 'subjects' ? 'block' : 'none';
    document.getElementById('panel-users').style.display = tab === 'users' ? 'block' : 'none';
    document.getElementById('panel-pending').style.display = tab === 'pending' ? 'block' : 'none';

    if (tab === 'subjects') loadSubjects();
    if (tab === 'users') loadUsers();
    if (tab === 'pending') loadPendingMentors();
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

// === Pending Mentors ===

async function loadPendingBadge() {
    const pending = await API.get('/api/admin/mentors/pending');
    const badge = document.getElementById('pending-badge');
    if (Array.isArray(pending) && pending.length > 0) {
        badge.textContent = pending.length;
        badge.style.display = 'inline';
    } else {
        badge.style.display = 'none';
    }
}

async function loadPendingMentors() {
    const container = document.getElementById('pending-list');
    container.innerHTML = '<p class="text-muted">Loading...</p>';

    const mentors = await API.get('/api/admin/mentors/pending');

    if (!Array.isArray(mentors) || mentors.length === 0) {
        container.innerHTML = `
            <div class="empty-state" style="padding:3rem 0;">
                <div class="empty-icon">✅</div>
                <h3>All caught up!</h3>
                <p class="text-muted">No pending mentor applications.</p>
            </div>`;
        return;
    }

    container.innerHTML = mentors.map(m => {
        const expLine = m.yearsOfExperience > 0
            ? `<div class="detail-row"><strong>Field:</strong> ${m.fieldOfExpertise || '—'} &nbsp;|&nbsp; <strong>Experience:</strong> ${m.yearsOfExperience} year(s)</div>`
            : `<div class="detail-row"><strong>Motivation:</strong> ${m.motivationToTeach || '—'}</div>`;

        return `
        <div class="mentor-application-card" id="card-${m.id}">
            <h4>${m.name} <span style="font-weight:400;color:rgba(255,255,255,0.45);">@${m.username}</span></h4>
            <div class="meta">${m.email} &nbsp;·&nbsp; Age ${m.age}</div>
            <div class="detail-row"><strong>Diploma:</strong> ${m.diplomaInfo || '—'}</div>
            ${expLine}
            ${m.bio ? `<div class="detail-row"><strong>Bio:</strong> ${m.bio}</div>` : ''}
            <div class="actions">
                <button class="btn btn-primary btn-sm" onclick="approveMentor(${m.id})">✓ Approve</button>
                <button class="btn btn-danger btn-sm" onclick="rejectMentor(${m.id})">✗ Reject</button>
            </div>
        </div>`;
    }).join('');
}

async function approveMentor(id) {
    await API.put(`/api/admin/mentors/${id}/approve`, {});
    API.showAlert('Mentor approved successfully!', 'success');
    loadPendingMentors();
    loadPendingBadge();
}

async function rejectMentor(id) {
    if (!confirm('Reject this mentor application?')) return;
    await API.put(`/api/admin/mentors/${id}/reject`, {});
    API.showAlert('Mentor application rejected.', 'error');
    loadPendingMentors();
    loadPendingBadge();
}
