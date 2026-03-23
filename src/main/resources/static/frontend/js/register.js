document.addEventListener('DOMContentLoaded', () => {
    const studentBtn = document.getElementById("studentBtn");
    const mentorBtn = document.getElementById("mentorBtn");
    const mentorFields = document.getElementById("mentorFields");
    const registerForm = document.querySelector('form');

    let currentRole = "STUDENT"; // Начална роля

    // Role Switching Logic
    studentBtn.onclick = () => {
        currentRole = "STUDENT";
        studentBtn.classList.add("active");
        mentorBtn.classList.remove("active");
        mentorFields.style.display = "none";
    };

    mentorBtn.onclick = () => {
        currentRole = "MENTOR";
        mentorBtn.classList.add("active");
        studentBtn.classList.remove("active");
        mentorFields.style.display = "block";
    };

    // Registration Logic
    registerForm.addEventListener('submit', async (e) => {
        e.preventDefault();

        const password = document.getElementById("password").value;
        const confirmPass = document.getElementById("confirmPassword").value;

        if (password !== confirmPass) {
            alert("Passwords do not match!");
            return;
        }

        // 1. Събиране на основните данни
        const formData = {
            fullName: registerForm.querySelector('input[placeholder="Enter your name"]').value,
            username: registerForm.querySelector('input[placeholder="Choose username"]').value,
            email: registerForm.querySelector('input[placeholder="Enter your email"]').value,
            age: registerForm.querySelector('input[placeholder="Your age"]').value,
            password: password,
            role: currentRole
        };

        // 2. Добавяне на специфични полета, ако е Ментор
        if (currentRole === "MENTOR") {
            formData.bio = mentorFields.querySelector('textarea[placeholder*="Tell students"]').value;
            formData.subject = mentorFields.querySelector('input[placeholder*="Example: Math"]').value;
            formData.experience = mentorFields.querySelector('input[placeholder*="Example: 5 years"]').value;
            formData.additionalInfo = mentorFields.querySelectorAll('textarea')[1].value;
        }

        try {
            // 3. Изпращане към Java Backend (промени URL-а спрямо твоя контролер)
            const response = await fetch('http://localhost:8080/api/auth/register', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(formData)
            });

            if (response.ok) {
                const result = await response.json();
                alert("Registration successful! Please login.");
                window.location.href = "../pages/login.html";
            } else {
                const errorData = await response.json();
                alert("Registration failed: " + (errorData.message || "Unknown error"));
            }
        } catch (error) {
            console.error("Connection error:", error);
            alert("Could not connect to the server. Is your Java app running?");
        }
    });
});

function togglePassword() {
    const pass = document.getElementById("password");
    const confirm = document.getElementById("confirmPassword");
    const type = pass.type === "password" ? "text" : "password";
    pass.type = type;
    if(confirm) confirm.type = type;
}
