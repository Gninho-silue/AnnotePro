// Annotation timer functionality
document.addEventListener('DOMContentLoaded', function () {
    let secondsElapsed = 0;
    let timerRunning = true;
    const timerDisplay = document.getElementById('annotationTimer');
    const hiddenInput = document.getElementById('elapsedSeconds');
    const formHiddenInput = document.getElementById('formElapsedSeconds');
    const toggleButton = document.getElementById('toggleTimer');
    const progressBar = document.getElementById('progressBar');
    let timerInterval = null;

    // Temps estimé pour une annotation (en secondes)
    const estimatedTime = 300; // 5 minutes

    function updateTimer() {
        if (timerRunning) {
            secondsElapsed++;
            const minutes = String(Math.floor(secondsElapsed / 60)).padStart(2, '0');
            const seconds = String(secondsElapsed % 60).padStart(2, '0');
            timerDisplay.textContent = `${minutes}:${seconds}`;
            timerDisplay.setAttribute('aria-label', `Temps écoulé: ${minutes} minutes ${seconds} secondes`);
            hiddenInput.value = secondsElapsed;
            formHiddenInput.value = secondsElapsed;

            // Mettre à jour la barre de progression
            const progress = Math.min((secondsElapsed / estimatedTime) * 100, 100);
            progressBar.style.width = `${progress}%`;
            progressBar.setAttribute('aria-valuenow', progress);

            // Changer la couleur de la barre de progression en fonction du temps
            if (progress >= 100) {
                progressBar.classList.remove('bg-primary');
                progressBar.classList.add('bg-warning');
            } else if (progress >= 75) {
                progressBar.classList.remove('bg-primary');
                progressBar.classList.add('bg-info');
            }
        }
    }

    // Démarrer le minuteur
    if (timerDisplay && hiddenInput && formHiddenInput && toggleButton) {
        timerInterval = setInterval(updateTimer, 1000);

        toggleButton.addEventListener('click', function () {
            timerRunning = !timerRunning;
            if (timerRunning) {
                toggleButton.innerHTML = '<i class="bi bi-pause-fill"></i> Pause';
                toggleButton.classList.remove('btn-outline-success');
                toggleButton.classList.add('btn-outline-secondary');
                toggleButton.setAttribute('aria-label', 'Mettre en pause le minuteur');
                progressBar.classList.remove('progress-bar-animated');
            } else {
                toggleButton.innerHTML = '<i class="bi bi-play-fill"></i> Reprendre';
                toggleButton.classList.remove('btn-outline-secondary');
                toggleButton.classList.add('btn-outline-success');
                toggleButton.setAttribute('aria-label', 'Reprendre le minuteur');
                progressBar.classList.add('progress-bar-animated');
            }
        });
    }

    // Arrêter le minuteur lors de la soumission du formulaire
    const annotationForm = document.getElementById('annotationForm');
    if (annotationForm) {
        annotationForm.addEventListener('submit', function () {
            if (timerInterval) {
                clearInterval(timerInterval);
            }
            // Marquer la barre de progression comme complétée
            progressBar.style.width = '100%';
            progressBar.classList.remove('bg-primary', 'bg-info', 'bg-warning');
            progressBar.classList.add('bg-success');
            progressBar.classList.remove('progress-bar-animated');
        });
    }
}); 