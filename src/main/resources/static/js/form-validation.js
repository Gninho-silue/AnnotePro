// Form validation functionality
document.addEventListener('DOMContentLoaded', function () {
    const form = document.querySelector('form');
    if (form) {
        form.addEventListener('submit', function(event) {
            const username = document.getElementById('username')?.value.trim();
            const firstName = document.getElementById('firstName')?.value.trim();
            const lastName = document.getElementById('lastName')?.value.trim();
            const email = document.getElementById('email')?.value.trim();
            const role = document.getElementById('role')?.value;

            if (username && firstName && lastName && email && role) {
                // Email validation
                const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
                if (!emailRegex.test(email)) {
                    event.preventDefault();
                    alert('Veuillez entrer une adresse email valide.');
                    return false;
                }
            }
        });
    }
}); 