// LearnFast — Home page interactions + scroll reveal

document.addEventListener('DOMContentLoaded', () => {
    // Scroll reveal animation
    const observerOptions = {
        root: null,
        rootMargin: '0px 0px -80px 0px',
        threshold: 0.1
    };

    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('revealed');
                observer.unobserve(entry.target);
            }
        });
    }, observerOptions);

    // Observe all elements with scroll-reveal class
    document.querySelectorAll('.scroll-reveal').forEach(el => {
        observer.observe(el);
    });

    // Observe glass-cards and feature-cards for scroll reveal
    document.querySelectorAll('.glass-card.feature-card, .glass-card.animate-in, .features-section .glass-card').forEach(el => {
        el.classList.add('scroll-reveal');
        observer.observe(el);
    });

    // Animate stat numbers with counter effect
    const statNumbers = document.querySelectorAll('.stat-number');
    const statsObserver = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                animateCounter(entry.target);
                statsObserver.unobserve(entry.target);
            }
        });
    }, { threshold: 0.5 });

    statNumbers.forEach(el => statsObserver.observe(el));

    // Navbar scroll effect
    let lastScroll = 0;
    window.addEventListener('scroll', () => {
        const navbar = document.getElementById('navbar');
        if (!navbar) return;

        const scrollY = window.scrollY;
        if (scrollY > 50) {
            navbar.style.background = 'rgba(10, 14, 26, 0.95)';
            navbar.style.boxShadow = '0 4px 20px rgba(0, 0, 0, 0.3)';
            navbar.style.padding = '0.75rem 2rem';
        } else {
            navbar.style.background = 'rgba(10, 14, 26, 0.8)';
            navbar.style.boxShadow = 'none';
            navbar.style.padding = '1rem 2rem';
        }
        lastScroll = scrollY;
    });
});

function animateCounter(el) {
    const text = el.textContent.trim();
    const match = text.match(/^(\d+)(.*)$/);
    if (!match) return;

    const target = parseInt(match[1]);
    const suffix = match[2]; // e.g. '+', 'K+', etc.
    const duration = 1500;
    const startTime = performance.now();

    function update(currentTime) {
        const elapsed = currentTime - startTime;
        const progress = Math.min(elapsed / duration, 1);
        // Ease out cubic
        const eased = 1 - Math.pow(1 - progress, 3);
        const current = Math.floor(eased * target);
        el.textContent = current + suffix;

        if (progress < 1) {
            requestAnimationFrame(update);
        }
    }

    requestAnimationFrame(update);
}
