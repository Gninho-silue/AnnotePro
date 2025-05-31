// Annotation interface enhancements
document.addEventListener('DOMContentLoaded', function () {
    const annotationForm = document.getElementById('annotationForm');
    const categorySelect = document.getElementById('categoryId');
    const previousBtn = document.querySelector('a[href*="/previous"]');
    const nextBtn = document.querySelector('a[href*="/next"]');
    let hasUnsavedChanges = false;

    // Keyboard shortcuts
    document.addEventListener('keydown', function(e) {
        // Alt + Left Arrow: Previous task
        if (e.altKey && e.key === 'ArrowLeft' && previousBtn) {
            e.preventDefault();
            if (hasUnsavedChanges) {
                if (confirm('Vous avez des modifications non sauvegardées. Voulez-vous vraiment quitter ?')) {
                    previousBtn.click();
                }
            } else {
                previousBtn.click();
            }
        }
        // Alt + Right Arrow: Next task
        else if (e.altKey && e.key === 'ArrowRight' && nextBtn) {
            e.preventDefault();
            if (hasUnsavedChanges) {
                if (confirm('Vous avez des modifications non sauvegardées. Voulez-vous vraiment quitter ?')) {
                    nextBtn.click();
                }
            } else {
                nextBtn.click();
            }
        }
        // Alt + S: Save annotation
        else if (e.altKey && e.key === 's' && annotationForm) {
            e.preventDefault();
            annotationForm.submit();
        }
    });

    // Track form changes
    if (categorySelect) {
        categorySelect.addEventListener('change', function() {
            hasUnsavedChanges = true;
            // Visual feedback for selection
            this.classList.add('is-valid');
            setTimeout(() => this.classList.remove('is-valid'), 1000);
        });
    }

    // Warn before leaving with unsaved changes
    window.addEventListener('beforeunload', function(e) {
        if (hasUnsavedChanges) {
            e.preventDefault();
            e.returnValue = '';
        }
    });

    // Reset unsaved changes flag on form submit
    if (annotationForm) {
        annotationForm.addEventListener('submit', function() {
            hasUnsavedChanges = false;
        });
    }

    // Add tooltips for keyboard shortcuts
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });

    // Add visual feedback for navigation buttons
    [previousBtn, nextBtn].forEach(btn => {
        if (btn) {
            btn.addEventListener('mouseover', function() {
                this.classList.add('btn-primary');
                this.classList.remove('btn-outline-primary');
            });
            btn.addEventListener('mouseout', function() {
                this.classList.remove('btn-primary');
                this.classList.add('btn-outline-primary');
            });
        }
    });
}); 