// ============================================================
// Hospital Management System – Main JavaScript
// ============================================================

document.addEventListener('DOMContentLoaded', function () {

    // ---------- Sidebar toggle (mobile) ----------
    const sidebar   = document.querySelector('.sidebar');
    const toggleBtn = document.querySelector('.sidebar-toggle');
    const overlay   = document.querySelector('.sidebar-overlay');

    if (toggleBtn) {
        toggleBtn.addEventListener('click', function () {
            if (sidebar) sidebar.classList.toggle('open');
            if (overlay) overlay.classList.toggle('show');
        });
    }

    if (overlay) {
        overlay.addEventListener('click', function () {
            if (sidebar) sidebar.classList.remove('open');
            overlay.classList.remove('show');
        });
    }

    // ---------- Active sidebar link highlighting ----------
    const currentPath = window.location.pathname;
    let exactMatch = false;

    document.querySelectorAll('.sidebar-nav a').forEach(function (link) {
        const href = link.getAttribute('href');
        if (href === currentPath) {
            link.classList.add('active');
            exactMatch = true;
        }
    });

    if (!exactMatch && currentPath !== '/') {
        document.querySelectorAll('.sidebar-nav a').forEach(function (link) {
            const href = link.getAttribute('href');
            if (href && href !== '/' && currentPath.startsWith(href)) {
                link.classList.add('active');
            }
        });
    }

    // ---------- Auto-dismiss temporary success & info alerts after 6 seconds ----------
    setTimeout(function () {
        document.querySelectorAll('.alert-dismissible.alert-success, .alert-dismissible.alert-info').forEach(function (alert) {
            try {
                if (window.bootstrap && bootstrap.Alert) {
                    const bsAlert = bootstrap.Alert.getOrCreateInstance(alert);
                    bsAlert.close();
                }
            } catch (e) {
                // Ignore if bootstrap is not available
            }
        });
    }, 6000);
});
