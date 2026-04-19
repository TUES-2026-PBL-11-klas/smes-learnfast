// LearnFast — Auth (Register + Login)

function selectRole(role) {
    document.querySelectorAll('.role-option').forEach(el => el.classList.remove('selected'));
    document.querySelector(`[data-role="${role}"]`).classList.add('selected');
    document.getElementById('reg-role').value = role;

    const mentorFields = document.getElementById('mentor-fields');
    if (mentorFields) {
        mentorFields.style.display = role === 'mentor' ? 'block' : 'none';
        if (role === 'mentor') {
            // show the correct sub-field based on current years value
            const years = parseInt(document.getElementById('reg-years')?.value) || 0;
            document.getElementById('field-of-expertise-group').style.display = years > 0 ? 'block' : 'none';
            document.getElementById('motivation-group').style.display = years === 0 ? 'block' : 'none';
        }
    }
}

// Register form
const registerForm = document.getElementById('register-form');
if (registerForm) {
    registerForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const btn = registerForm.querySelector('button[type="submit"]');
        btn.disabled = true;
        btn.textContent = 'Creating...';

        try {
            const role = document.getElementById('reg-role').value;
            const data = {
                name: document.getElementById('reg-name').value.trim(),
                username: document.getElementById('reg-username').value.trim(),
                email: document.getElementById('reg-email').value.trim(),
                age: parseInt(document.getElementById('reg-age').value),
                password: document.getElementById('reg-password').value,
                bio: document.getElementById('reg-bio').value.trim(),
                role
            };

            if (role === 'mentor') {
                const years = parseInt(document.getElementById('reg-years').value);
                data.diplomaInfo = document.getElementById('reg-diploma').value.trim();
                data.yearsOfExperience = isNaN(years) ? 0 : years;
                data.fieldOfExpertise = document.getElementById('reg-field').value.trim() || null;
                data.motivationToTeach = document.getElementById('reg-motivation').value.trim() || null;
            }

            const res = await API.post('/api/auth/register', data);

            if (res.error) {
                API.showAlert(res.error, 'error');
                btn.disabled = false;
                btn.textContent = 'Create Account';
                return;
            }

            if (res.status === 'PENDING_APPROVAL') {
                registerForm.style.display = 'none';
                document.querySelector('.auth-subtitle').style.display = 'none';
                document.querySelector('h2').textContent = 'Application Submitted!';
                document.getElementById('alert-container').innerHTML = `
                    <div style="text-align:center; padding: 1.5rem 0;">
                        <div style="font-size:3rem; margin-bottom:1rem;">⏳</div>
                        <p style="font-size:1.05rem; margin-bottom:0.5rem;">
                            Thank you, <strong>${res.name}</strong>!
                        </p>
                        <p style="color:rgba(255,255,255,0.6); font-size:0.9rem; margin-bottom:1.5rem;">
                            Your mentor application is currently under review.<br>
                            An admin will approve your profile shortly.
                        </p>
                        <a href="/frontend/pages/login.html" class="btn btn-primary">Back to Login</a>
                    </div>`;
                return;
            }

            window.location.href = '/frontend/pages/dashboard.html';
        } catch (err) {
            API.showAlert('Something went wrong. Please try again.', 'error');
            btn.disabled = false;
            btn.textContent = 'Create Account';
        }
    });
}

// Login form
const loginForm = document.getElementById('login-form');
if (loginForm) {
    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const btn = loginForm.querySelector('button[type="submit"]');
        btn.disabled = true;
        btn.textContent = 'Logging in...';

        try {
            const data = {
                username: document.getElementById('login-username').value.trim(),
                password: document.getElementById('login-password').value
            };

            const res = await API.post('/api/auth/login', data);

            if (res.error) {
                API.showAlert(res.error, 'error');
                btn.disabled = false;
                btn.textContent = 'Log In';
                return;
            }

            // Redirect based on role
            if (res.role === 'admin') {
                window.location.href = '/frontend/pages/admin.html';
            } else {
                window.location.href = '/frontend/pages/dashboard.html';
            }
        } catch (err) {
            API.showAlert('Something went wrong. Please try again.', 'error');
            btn.disabled = false;
            btn.textContent = 'Log In';
        }
    });
}
