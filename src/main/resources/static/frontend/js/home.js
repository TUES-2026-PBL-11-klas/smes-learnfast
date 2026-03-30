document.addEventListener('DOMContentLoaded', () => {
    const searchInput = document.querySelector('.search-box input');
    const categories = document.querySelectorAll('.category');

    // Search logic (simulated)
    searchInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            alert("Searching for: " + searchInput.value);
        }
    });

    // Category click logic
    categories.forEach(cat => {
        cat.addEventListener('click', () => {
            const subject = cat.querySelector('h3').innerText;
            alert("Opening mentors for: " + subject);
        });
    });

    // Profile button redirect
    document.querySelector('.profile-btn').onclick = () => {
        window.location.href = "login.html";
    };
});
