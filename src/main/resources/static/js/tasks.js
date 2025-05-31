// Tasks-specific functionality
document.addEventListener('DOMContentLoaded', function () {
    // Toast for urgent tasks
    const urgentCount = document.getElementById('deadlineToast') ? 
        parseInt(document.getElementById('deadlineToast').getAttribute('data-urgent-count') || '0') : 0;
    
    if (urgentCount > 0) {
        const toastElement = document.getElementById('deadlineToast');
        if (toastElement) {
            const toast = new bootstrap.Toast(toastElement);
            toast.show();
        }
    }

    // Handle collapsible sections
    document.querySelectorAll('.card-header[data-bs-toggle="collapse"]').forEach(header => {
        header.addEventListener('click', function() {
            const icon = this.querySelector('.bi-chevron-down');
            if (icon) {
                icon.classList.toggle('bi-chevron-down');
                icon.classList.toggle('bi-chevron-up');
            }
        });
    });
}); 