// Finance Dashboard JavaScript

document.addEventListener('DOMContentLoaded', function() {
    console.log('Finance Dashboard loaded');
    
    // Initialize tooltips
    initializeTooltips();
    
    // Initialize search functionality
    initializeSearch();
    
    // Initialize filter functionality
    initializeFilters();
    
    // Add fade-in animation to cards
    addFadeInAnimation();
    
    // Initialize copy to clipboard functionality
    initializeCopyToClipboard();
});

/**
 * Initialize Bootstrap tooltips
 */
function initializeTooltips() {
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });
}

/**
 * Initialize search functionality
 */
function initializeSearch() {
    const searchInputs = document.querySelectorAll('input[type="text"][id*="Search"]');
    
    searchInputs.forEach(input => {
        input.addEventListener('input', debounce(function(e) {
            const searchTerm = e.target.value.toLowerCase();
            const targetTable = e.target.id.replace('Search', '') + 'Table';
            const table = document.getElementById(targetTable);
            
            if (table) {
                const rows = table.querySelectorAll('tbody tr');
                rows.forEach(row => {
                    const text = row.textContent.toLowerCase();
                    row.style.display = text.includes(searchTerm) ? '' : 'none';
                });
                
                updateSearchResults(table, searchTerm);
            }
        }, 300));
    });
}

/**
 * Initialize filter functionality
 */
function initializeFilters() {
    const filterSelects = document.querySelectorAll('select[id*="Filter"]');
    
    filterSelects.forEach(select => {
        select.addEventListener('change', function(e) {
            const filterValue = e.target.value;
            const rows = document.querySelectorAll('.transaction-row, .account-row');
            
            rows.forEach(row => {
                if (!filterValue) {
                    row.style.display = '';
                } else {
                    const rowType = row.getAttribute('data-type');
                    row.style.display = rowType === filterValue ? '' : 'none';
                }
            });
        });
    });
}

/**
 * Add fade-in animation to cards
 */
function addFadeInAnimation() {
    const cards = document.querySelectorAll('.card');
    cards.forEach((card, index) => {
        card.style.opacity = '0';
        card.style.transform = 'translateY(20px)';
        
        setTimeout(() => {
            card.style.transition = 'all 0.5s ease';
            card.style.opacity = '1';
            card.style.transform = 'translateY(0)';
        }, index * 100);
    });
}

/**
 * Initialize copy to clipboard functionality
 */
function initializeCopyToClipboard() {
    const copyButtons = document.querySelectorAll('[data-copy]');
    
    copyButtons.forEach(button => {
        button.addEventListener('click', function() {
            const textToCopy = this.getAttribute('data-copy');
            
            if (navigator.clipboard) {
                navigator.clipboard.writeText(textToCopy).then(() => {
                    showToast('Copied to clipboard!', 'success');
                });
            } else {
                // Fallback for older browsers
                const textArea = document.createElement('textarea');
                textArea.value = textToCopy;
                document.body.appendChild(textArea);
                textArea.select();
                document.execCommand('copy');
                document.body.removeChild(textArea);
                showToast('Copied to clipboard!', 'success');
            }
        });
    });
}

/**
 * Update search results display
 */
function updateSearchResults(table, searchTerm) {
    const rows = table.querySelectorAll('tbody tr');
    const visibleRows = Array.from(rows).filter(row => row.style.display !== 'none');
    
    // You could add a results counter here if needed
    console.log(`Found ${visibleRows.length} results for "${searchTerm}"`);
}

/**
 * Show toast notification
 */
function showToast(message, type = 'info') {
    // Create toast element
    const toast = document.createElement('div');
    toast.className = `toast align-items-center text-white bg-${type} border-0`;
    toast.setAttribute('role', 'alert');
    toast.setAttribute('aria-live', 'assertive');
    toast.setAttribute('aria-atomic', 'true');
    
    toast.innerHTML = `
        <div class="d-flex">
            <div class="toast-body">
                ${message}
            </div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
        </div>
    `;
    
    // Add to toast container or create one
    let toastContainer = document.querySelector('.toast-container');
    if (!toastContainer) {
        toastContainer = document.createElement('div');
        toastContainer.className = 'toast-container position-fixed bottom-0 end-0 p-3';
        document.body.appendChild(toastContainer);
    }
    
    toastContainer.appendChild(toast);
    
    // Initialize and show toast
    const bsToast = new bootstrap.Toast(toast);
    bsToast.show();
    
    // Remove from DOM after it's hidden
    toast.addEventListener('hidden.bs.toast', () => {
        toast.remove();
    });
}

/**
 * Debounce function to limit search frequency
 */
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

/**
 * Format currency
 */
function formatCurrency(amount) {
    return new Intl.NumberFormat('en-US', {
        style: 'currency',
        currency: 'USD'
    }).format(amount);
}

/**
 * Format date
 */
function formatDate(dateString) {
    return new Date(dateString).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
    });
}

/**
 * Show loading state
 */
function showLoading(element) {
    element.classList.add('loading');
}

/**
 * Hide loading state
 */
function hideLoading(element) {
    element.classList.remove('loading');
}

/**
 * API error handler
 */
function handleApiError(error, context = 'operation') {
    console.error(`API Error during ${context}:`, error);
    showToast(`Failed to complete ${context}. Please try again.`, 'danger');
}

// Export functions for use in other scripts
window.FinanceDashboard = {
    showToast,
    formatCurrency,
    formatDate,
    showLoading,
    hideLoading,
    handleApiError
};