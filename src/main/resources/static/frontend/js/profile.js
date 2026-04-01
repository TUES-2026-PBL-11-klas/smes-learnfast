// LearnFast — Profile, Reviews, Editing

let isOwner = false;
let targetUserId = null;
let profileData = null;
let loggedInUser = null;
let allAdminSubjects = [];
let selectedSubjectsCache = new Set();
let reviewInputRating = 0;

(async function initProfile() {
    const params = new URLSearchParams(window.location.search);
    targetUserId = params.get('id');

    loggedInUser = await API.get('/api/auth/me').catch(() => null);
    if (loggedInUser && !loggedInUser.error) {
        API.updateNav(loggedInUser);
        if (!targetUserId || targetUserId === 'me') {
            targetUserId = loggedInUser.id;
        }
    } else if (!targetUserId || targetUserId === 'me') {
        window.location.href = '/frontend/pages/login.html';
        return;
    }

    isOwner = (loggedInUser && !loggedInUser.error && loggedInUser.id == targetUserId);

    await loadProfileDetails();
    if (profileData.role === 'mentor') {
        await loadReviews();
    }
    
    setupReviewStars();
    setupEditForm();
})();

async function loadProfileDetails() {
    profileData = await API.get(`/api/mentors/${targetUserId}`); // also returns students if fetched by id
    
    if (!profileData || profileData.error) {
        document.getElementById('profile-header').innerHTML = `
            <div class="empty-state">
                <div class="empty-icon">😕</div>
                <h3>User not found</h3>
                <a href="/frontend/pages/index.html" class="btn btn-secondary mt-2">Go Home</a>
            </div>`;
        return;
    }

    const { name, username, age, email, role, bio, avatarUrl, subjects, averageRating, reviewCount } = profileData;
    
    let avatarHtml;
    if (avatarUrl && avatarUrl.trim() !== '') {
        avatarHtml = `<img src="${avatarUrl}" alt="${name}" style="width:80px;height:80px;border-radius:50%;object-fit:cover;margin:0 auto 1rem;">`;
    } else {
        const initials = name.split(' ').map(w => w[0]).join('').toUpperCase();
        avatarHtml = `<div class="avatar avatar-lg" style="margin: 0 auto 1rem;">${initials}</div>`;
    }

    let ratingHtml = '';
    if (role === 'mentor' && reviewCount > 0) {
        ratingHtml = `<div style="color:var(--accent-orange);font-weight:bold;margin:0.25rem 0;">⭐ ${averageRating} / 5.0 (${reviewCount} reviews)</div>`;
    } else if (role === 'mentor') {
        ratingHtml = `<div style="color:var(--text-muted);font-size:0.85rem;margin:0.25rem 0;">No reviews yet</div>`;
    }

    const subjectsHtml = (subjects || []).map(s => `<span class="badge badge-blue">${s}</span>`).join('');

    let actns = '';
    if (isOwner) {
        actns = `<button class="btn btn-secondary mt-2" onclick="openEditModal()">✏️ Edit Profile</button>`;
    } else {
        actns = `
            <div class="profile-actions">
                <a href="/frontend/pages/chat.html?user=${targetUserId}" class="btn btn-primary">💬 Chat</a>
                ${role === 'mentor' ? `<button class="btn btn-accent" onclick="requestSession(${targetUserId})">📹 Call</button>` : ''}
            </div>
        `;
    }

    document.getElementById('profile-header').innerHTML = `
        ${avatarHtml}
        <h1>${name}</h1>
        <p class="text-muted">@${username}</p>
        ${ratingHtml}
        <div class="profile-info">
            <div class="profile-info-item">🎓 <span style="text-transform:capitalize;">${role}</span></div>
            <div class="profile-info-item">📧 ${email}</div>
            <div class="profile-info-item">🎂 ${age} years old</div>
        </div>
        ${subjectsHtml ? `<div class="profile-subjects mt-2">${subjectsHtml}</div>` : ''}
        ${actns}
    `;

    if (bio) {
        document.getElementById('profile-bio').style.display = 'block';
        document.getElementById('profile-bio').innerHTML = `<p>${bio}</p>`;
    } else {
        document.getElementById('profile-bio').style.display = 'none';
    }
}

async function requestSession(mentorId) {
    if (!loggedInUser || loggedInUser.error) {
        alert('Please log in first.');
        window.location.href = '/frontend/pages/login.html';
        return;
    }
    try {
        const res = await API.post('/api/sessions', { mentorId });
        if (res.error) { API.showAlert(res.error, 'error'); return; }
        API.showAlert('Session request sent! Mentor will be notified.', 'success');
        setTimeout(() => window.location.href = '/frontend/pages/dashboard.html', 1500);
    } catch (e) {
        console.error(e);
    }
}

// === Reviews Logic ===
async function loadReviews() {
    const section = document.getElementById('reviews-section');
    section.style.display = 'block';
    
    // Show form if logged in student and not own profile
    if (!isOwner && loggedInUser && !loggedInUser.error && loggedInUser.role === 'student') {
        document.getElementById('review-form-container').style.display = 'block';
    }

    const reviews = await API.get(`/api/reviews/mentor/${targetUserId}`);
    const list = document.getElementById('reviews-list');

    if (!Array.isArray(reviews) || reviews.length === 0) {
        list.innerHTML = `<div class="text-muted text-center" style="padding: 1rem;">No reviews yet.</div>`;
        return;
    }

    list.innerHTML = reviews.map(r => {
        const stars = '★'.repeat(r.rating) + '☆'.repeat(5 - r.rating);
        let sAvatar = '';
        if (r.studentAvatarUrl && r.studentAvatarUrl.trim() !== '') {
            sAvatar = `<img src="${r.studentAvatarUrl}" style="width:30px;height:30px;border-radius:50%;object-fit:cover;">`;
        } else {
            const intls = r.studentName.substring(0,2).toUpperCase();
            sAvatar = `<div class="avatar avatar-sm" style="width:30px;height:30px;font-size:0.75rem;">${intls}</div>`;
        }

        return `
            <div class="review-card">
                <div class="review-header">
                    ${sAvatar}
                    <div class="review-meta">
                        <h4>${r.studentName}</h4>
                        <div class="date">${new Date(r.createdAt).toLocaleDateString()}</div>
                    </div>
                    <div style="margin-left:auto;color:var(--accent-orange);">${stars}</div>
                </div>
                ${r.comment ? `<p style="font-size:0.9rem;margin-top:0.5rem;color:var(--text-secondary);">${r.comment}</p>` : ''}
            </div>
        `;
    }).join('');
}

function setupReviewStars() {
    const stars = document.querySelectorAll('#review-stars-input .star');
    stars.forEach(s => {
        s.addEventListener('click', () => {
            reviewInputRating = parseInt(s.getAttribute('data-val'));
            document.getElementById('review-rating').value = reviewInputRating;
            stars.forEach(st => {
                if (parseInt(st.getAttribute('data-val')) <= reviewInputRating) {
                    st.classList.add('active');
                } else {
                    st.classList.remove('active');
                }
            });
        });
    });

    document.getElementById('review-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        const rating = parseInt(document.getElementById('review-rating').value);
        const comment = document.getElementById('review-comment').value.trim();

        if (rating === 0) {
            API.showAlert('Please select a star rating', 'error');
            return;
        }

        const res = await API.post('/api/reviews', { mentorId: targetUserId, rating, comment });
        if (res.error) {
            API.showAlert(res.error, 'error');
            return;
        }

        API.showAlert('Review published!', 'success');
        document.getElementById('review-form-container').style.display = 'none';
        
        // Reload details to update average
        await loadProfileDetails();
        await loadReviews();
    });
}

// === Edit Profile Logic ===
async function openEditModal() {
    document.getElementById('edit-name').value = profileData.name;
    document.getElementById('edit-bio').value = profileData.bio || '';
    document.getElementById('edit-avatar').value = profileData.avatarUrl || '';

    if (profileData.role === 'mentor') {
        document.getElementById('edit-subjects-container').style.display = 'block';
        if (allAdminSubjects.length === 0) {
            allAdminSubjects = await API.get('/api/admin/subjects');
        }
        
        selectedSubjectsCache.clear();
        (profileData.subjects || []).forEach(sn => selectedSubjectsCache.add(sn));

        const c = document.getElementById('edit-subjects-list');
        c.innerHTML = allAdminSubjects.map(s => {
            const isSel = selectedSubjectsCache.has(s.name);
            return `<button type="button" class="btn btn-sm ${isSel ? 'btn-primary' : 'btn-secondary'}" onclick="toggleSubjectOption('${s.name}', ${s.id}, this)">${s.name}</button>`;
        }).join('');
    }

    document.getElementById('edit-modal').classList.add('show');
}

function closeEditModal() {
    document.getElementById('edit-modal').classList.remove('show');
}

function toggleSubjectOption(name, id, btnEl) {
    if (selectedSubjectsCache.has(name)) {
        selectedSubjectsCache.delete(name);
        btnEl.classList.remove('btn-primary');
        btnEl.classList.add('btn-secondary');
    } else {
        selectedSubjectsCache.add(name);
        btnEl.classList.add('btn-primary');
        btnEl.classList.remove('btn-secondary');
    }
}

function setupEditForm() {
    document.getElementById('edit-profile-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const payload = {
            name: document.getElementById('edit-name').value.trim(),
            bio: document.getElementById('edit-bio').value.trim(),
            avatarUrl: document.getElementById('edit-avatar').value.trim(),
            age: profileData.age
        };

        const res = await API.put('/api/profile', payload);
        if (res.error) { API.showAlert(res.error, 'error'); return; }

        if (profileData.role === 'mentor') {
            const subjectIdsToSave = allAdminSubjects
                .filter(s => selectedSubjectsCache.has(s.name))
                .map(s => s.id);
            await API.put('/api/profile/subjects', { subjectIds: subjectIdsToSave });
        }

        closeEditModal();
        API.showAlert('Profile updated!', 'success');
        
        // Reload
        await loadProfileDetails();
    });
}
