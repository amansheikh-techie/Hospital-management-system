// ============================================================
// Hospital Management System – Main JavaScript
// ============================================================

document.addEventListener('DOMContentLoaded', function () {

    // ---------- Sidebar toggle (mobile) ----------
    const sidebar      = document.querySelector('.sidebar');
    const toggleBtn    = document.querySelector('.sidebar-toggle');
    const overlay      = document.querySelector('.sidebar-overlay');

    if (toggleBtn) {
        toggleBtn.addEventListener('click', function () {
            sidebar.classList.toggle('open');
            if (overlay) overlay.classList.toggle('show');
        });
    }

    if (overlay) {
        overlay.addEventListener('click', function () {
            sidebar.classList.remove('open');
            overlay.classList.remove('show');
        });
    }

    // ---------- Active sidebar link highlighting ----------
    const currentPath = window.location.pathname;
    document.querySelectorAll('.sidebar-nav a').forEach(function (link) {
        if (link.getAttribute('href') === currentPath) {
            link.classList.add('active');
        }
    });
});
