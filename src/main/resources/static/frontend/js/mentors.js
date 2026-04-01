// LearnFast — Mentors listing

let allMentors = [];
let allSubjects = [];
let activeFilter = null;

(async function() {
    // Auth check (optional for viewing)
    try {
        const user = await API.get('/api/auth/me');
        if (user && !user.error) API.updateNav(user);
    } catch(e) {}

    // Load subjects for filters
    try {
        allSubjects = await API.get('/api/admin/subjects');
        renderSubjectFilters();
    } catch(e) {}

    // Load mentors
    try {
        allMentors = await API.get('/api/mentors');
        renderMentors(allMentors);
    } catch(e) {
        document.getElementById('mentors-grid').innerHTML =
            '<div class="empty-state" style="grid-column:1/-1;"><p>Failed to load mentors</p></div>';
    }

    // Search
    document.getElementById('search-input').addEventListener('input', (e) => {
        filterMentors();
    });
})();

function renderSubjectFilters() {
    if (!Array.isArray(allSubjects) || allSubjects.length === 0) return;
    const container = document.getElementById('subject-filters');
    container.innerHTML = `
        <button class="btn btn-sm ${!activeFilter ? 'btn-primary' : 'btn-secondary'}"
                onclick="setFilter(null)">All</button>
        ${allSubjects.map(s => `
            <button class="btn btn-sm ${activeFilter === s.name ? 'btn-primary' : 'btn-secondary'}"
                    onclick="setFilter('${s.name}')">${s.name}</button>
        `).join('')}
    `;
}

function setFilter(subject) {
    activeFilter = subject;
    renderSubjectFilters();
    filterMentors();
}

function filterMentors() {
    const query = document.getElementById('search-input').value.toLowerCase().trim();
    let filtered = allMentors;

    if (query) {
        filtered = filtered.filter(m =>
            m.name.toLowerCase().includes(query) ||
            m.username.toLowerCase().includes(query) ||
            (m.subjects && m.subjects.some(s => s.toLowerCase().includes(query)))
        );
    }

    if (activeFilter) {
        filtered = filtered.filter(m =>
            m.subjects && m.subjects.includes(activeFilter)
        );
    }

    renderMentors(filtered);
}

function renderMentors(mentors) {
    const grid = document.getElementById('mentors-grid');
    const empty = document.getElementById('no-mentors');

    if (!mentors || mentors.length === 0) {
        grid.innerHTML = '';
        empty.classList.remove('hidden');
        return;
    }

    empty.classList.add('hidden');
    grid.innerHTML = mentors.map(m => {
        const initials = m.name.split(' ').map(w => w[0]).join('').toUpperCase();
        const subjects = (m.subjects || []).map(s =>
            `<span class="badge badge-blue">${s}</span>`).join('');
        const bio = m.bio ? m.bio.substring(0, 120) + (m.bio.length > 120 ? '...' : '') : 'No bio yet';

        return `
            <div class="glass-card mentor-card animate-in">
                <div class="mentor-card-header">
                    <div class="avatar">${initials}</div>
                    <div class="mentor-card-info">
                        <h3>${m.name}</h3>
                        <span class="mentor-meta">@${m.username} · ${m.age} years old</span>
                    </div>
                </div>
                ${subjects ? `<div class="mentor-subjects">${subjects}</div>` : ''}
                <p class="mentor-bio">${bio}</p>
                <div class="mentor-actions">
                    <a href="/frontend/pages/profile.html?id=${m.id}" class="btn btn-secondary btn-sm">View Profile</a>
                    <a href="/frontend/pages/chat.html?user=${m.id}" class="btn btn-primary btn-sm">💬 Chat</a>
                </div>
            </div>
        `;
    }).join('');
}
