document.addEventListener('DOMContentLoaded', () => {
    const studentBtn = document.getElementById("studentBtn");
    const mentorBtn = document.getElementById("mentorBtn");
    const mentorFields = document.getElementById("mentorFields");
    const registerForm = document.querySelector('form');

    // Role Switching Logic
    studentBtn.onclick = () => {
        studentBtn.classList.add("active");
        mentorBtn.classList.remove("active");
        mentorFields.style.display = "none";
    };

    mentorBtn.onclick = () => {
        mentorBtn.classList.add("active");
        studentBtn.classList.remove("active");
        mentorFields.style.display = "block";
    };

    // Registration Logic
    registerForm.addEventListener('submit', (e) => {
        e.preventDefault();
        
        const pass = document.getElementById("password").value;
        const confirmPass = document.getElementById("confirmPassword").value;

        if (pass !== confirmPass) {
            alert("Passwords do not match!");
            return;
        }

        alert("Registration successful! Please login.");
        window.location.href = "login.html";
    });
});

function togglePassword() {
    const pass = document.getElementById("password");
    const confirm = document.getElementById("confirmPassword");
    const type = pass.type === "password" ? "text" : "password";
    pass.type = type;
    if(confirm) confirm.type = type;
}