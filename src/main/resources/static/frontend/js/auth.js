// LearnFast — Auth (Register + Login)

function selectRole(role) {
    document.querySelectorAll('.role-option').forEach(el => el.classList.remove('selected'));
    document.querySelector(`[data-role="${role}"]`).classList.add('selected');
    document.getElementById('reg-role').value = role;
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
            const data = {
                name: document.getElementById('reg-name').value.trim(),
                username: document.getElementById('reg-username').value.trim(),
                email: document.getElementById('reg-email').value.trim(),
                age: parseInt(document.getElementById('reg-age').value),
                password: document.getElementById('reg-password').value,
                bio: document.getElementById('reg-bio').value.trim(),
                role: document.getElementById('reg-role').value
            };

            const res = await API.post('/api/auth/register', data);

            if (res.error) {
                API.showAlert(res.error, 'error');
                btn.disabled = false;
                btn.textContent = 'Create Account';
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
