document.addEventListener('DOMContentLoaded', () => {
    const loginForm = document.querySelector('form');

    loginForm.addEventListener('submit', (e) => {
        e.preventDefault(); // Prevents page refresh

        const email = loginForm.querySelector('input[type="email"]').value;
        const password = document.getElementById('password').value;

        if (email && password) {
            // Success Simulation
            console.log("Logged in as:", email);
            alert("Welcome back!");
            window.location.href = "home.html";
        } else {
            alert("Please enter both email and password.");
        }
    });
});

// Shared Toggle Function
function togglePassword() {
    const passField = document.getElementById("password");
    passField.type = passField.type === "password" ? "text" : "password";
}
