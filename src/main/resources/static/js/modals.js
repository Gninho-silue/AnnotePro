// Modal handling functionality
document.addEventListener('DOMContentLoaded', function () {
    // Désactiver l'overlay lorsque les modals sont ouverts
    document.querySelectorAll('.modal').forEach(modal => {
        modal.addEventListener('show.bs.modal', function () {
            const overlay = document.getElementById('overlay');
            if (overlay) {
                overlay.classList.remove('active');
                document.body.classList.remove('overflow-hidden');
            }
        });
    });
}); 