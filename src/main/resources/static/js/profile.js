// Profile page functionality
document.addEventListener('DOMContentLoaded', function () {
    // Password strength validation
    const newPasswordInput = document.getElementById('newPassword');
    const passwordStrengthBar = document.getElementById('passwordStrengthBar');
    const passwordStrengthText = document.getElementById('passwordStrengthText');
    const passwordError = document.getElementById('passwordError');
    const passwordErrorMessage = document.getElementById('passwordErrorMessage');
    const passwordForm = document.getElementById('passwordForm');

    function updatePasswordStrength() {
        const password = newPasswordInput.value;
        let strengthScore = 0;

        // Strength criteria
        if (password.length >= 6) strengthScore++;
        if (/[A-Z]/.test(password)) strengthScore++;
        if (/[a-z]/.test(password)) strengthScore++;
        if (/\d/.test(password)) strengthScore++;
        if (/[!@#$%^&*(),.?":{}|<>]/.test(password)) strengthScore++;

        // Update bar and text
        const strengthPercentage = strengthScore * 20; // 20% per criterion
        passwordStrengthBar.style.width = strengthPercentage + '%';
        passwordStrengthBar.setAttribute('aria-valuenow', strengthPercentage);

        // Apply classes and text
        passwordStrengthBar.parentElement.classList.remove('password-strength-weak', 'password-strength-medium', 'password-strength-strong');
        if (strengthScore <= 2) {
            passwordStrengthBar.parentElement.classList.add('password-strength-weak');
            passwordStrengthText.textContent = 'Faible';
            passwordStrengthText.classList.add('text-danger');
            passwordStrengthText.classList.remove('text-warning', 'text-success');
        } else if (strengthScore <= 4) {
            passwordStrengthBar.parentElement.classList.add('password-strength-medium');
            passwordStrengthText.textContent = 'Moyen';
            passwordStrengthText.classList.add('text-warning');
            passwordStrengthText.classList.remove('text-danger', 'text-success');
        } else {
            passwordStrengthBar.parentElement.classList.add('password-strength-strong');
            passwordStrengthText.textContent = 'Fort';
            passwordStrengthText.classList.add('text-success');
            passwordStrengthText.classList.remove('text-danger', 'text-warning');
        }

        // Hide text if field is empty
        if (password.length === 0) {
            passwordStrengthText.textContent = 'Entrez un mot de passe';
            passwordStrengthText.classList.add('text-muted');
            passwordStrengthText.classList.remove('text-danger', 'text-warning', 'text-success');
            passwordStrengthBar.style.width = '0%';
        }
    }

    if (newPasswordInput) {
        newPasswordInput.addEventListener('input', updatePasswordStrength);
    }

    if (passwordForm) {
        passwordForm.addEventListener('submit', function(event) {
            const newPassword = newPasswordInput.value;
            const confirmPassword = document.getElementById('confirmPassword').value;

            // Check if passwords match
            if (newPassword !== confirmPassword) {
                event.preventDefault();
                passwordErrorMessage.textContent = 'Les mots de passe ne correspondent pas.';
                passwordError.classList.remove('d-none');
                return false;
            }

            // Password complexity validation
            const hasUpperCase = /[A-Z]/.test(newPassword);
            const hasLowerCase = /[a-z]/.test(newPassword);
            const hasNumbers = /\d/.test(newPassword);
            const hasSpecialChar = /[!@#$%^&*(),.?":{}|<>]/.test(newPassword);
            const isLongEnough = newPassword.length >= 6;

            if (!(hasUpperCase && hasLowerCase && hasNumbers && hasSpecialChar && isLongEnough)) {
                event.preventDefault();
                passwordErrorMessage.textContent = 'Le mot de passe ne respecte pas les critères de sécurité requis.';
                passwordError.classList.remove('d-none');
                return false;
            }
        });
    }
}); 