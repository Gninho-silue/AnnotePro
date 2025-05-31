// User selection and search functionality
document.addEventListener('DOMContentLoaded', function () {
    // DOM elements
    const userSearch = document.getElementById('userSearch');
    const userRows = document.querySelectorAll('.user-row');
    const checkboxes = document.querySelectorAll('input[name="annotatorIds"]');
    const selectedCount = document.getElementById('selectedCount');
    const selectAllBtn = document.getElementById('selectAll');
    const deselectAllBtn = document.getElementById('deselectAll');
    const assignForm = document.getElementById('assignForm');

    // Update selection count and row styles
    function updateSelectedCount() {
        const checked = document.querySelectorAll('input[name="annotatorIds"]:checked');
        selectedCount.textContent = checked.length;
        userRows.forEach(row => {
            const checkbox = row.querySelector('input[type="checkbox"]');
            row.classList.toggle('selected', checkbox.checked);
        });
    }

    // Initialize counter
    updateSelectedCount();

    // Filter users based on search
    userSearch.addEventListener('input', function () {
        const searchTerm = this.value.toLowerCase();
        userRows.forEach(row => {
            const username = row.querySelector('td:nth-child(1)').textContent.toLowerCase();
            const email = row.querySelector('td:nth-child(2)').textContent.toLowerCase();
            row.style.display = (username.includes(searchTerm) || email.includes(searchTerm)) ? '' : 'none';
        });
    });

    // Select all visible users
    selectAllBtn.addEventListener('click', function () {
        userRows.forEach(row => {
            if (row.style.display !== 'none') {
                const checkbox = row.querySelector('input[type="checkbox"]');
                checkbox.checked = true;
            }
        });
        updateSelectedCount();
    });

    // Deselect all users
    deselectAllBtn.addEventListener('click', function () {
        checkboxes.forEach(cb => cb.checked = false);
        updateSelectedCount();
    });

    // Update counter when checkboxes change
    checkboxes.forEach(cb => cb.addEventListener('change', updateSelectedCount));

    // Toggle checkbox when clicking on row
    userRows.forEach(row => {
        row.addEventListener('click', function (e) {
            if (e.target.tagName.toLowerCase() === 'input') return;
            const checkbox = row.querySelector('input[type="checkbox"]');
            checkbox.checked = !checkbox.checked;
            updateSelectedCount();
        });
    });

    // Validate form before submission
    assignForm.addEventListener('submit', function (e) {
        const checkedCount = document.querySelectorAll('input[name="annotatorIds"]:checked').length;
        if (checkedCount === 0) {
            e.preventDefault();
            alert('Veuillez sélectionner au moins un annotateur.');
        }
    });
}); 