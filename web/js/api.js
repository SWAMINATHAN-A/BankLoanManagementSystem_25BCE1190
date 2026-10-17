/**
 * Bank Loan Management System — Simple Client API Helper
 * Designed for college DBMS practical.
 */
const API = {
    TOKEN_KEY: 'blms_auth_token',
    USER_KEY: 'blms_user_data',

    getToken() {
        return localStorage.getItem(this.TOKEN_KEY);
    },

    getUser() {
        const u = localStorage.getItem(this.USER_KEY);
        try { return u ? JSON.parse(u) : null; } catch(e) { return null; }
    },

    setSession(token, user) {
        localStorage.setItem(this.TOKEN_KEY, token);
        localStorage.setItem(this.USER_KEY, JSON.stringify(user));
    },

    clearSession() {
        localStorage.removeItem(this.TOKEN_KEY);
        localStorage.removeItem(this.USER_KEY);
    },

    isAuthenticated() {
        return !!this.getToken();
    },

    async request(endpoint, options = {}) {
        const headers = {
            'Content-Type': 'application/json',
            ...(options.headers || {})
        };

        const token = this.getToken();
        if (token) {
            headers['X-Session-Token'] = token;
        }

        try {
            const response = await fetch(endpoint, {
                ...options,
                headers
            });

            const data = await response.json().catch(() => ({}));

            if (response.status === 401 && !endpoint.includes('/api/login')) {
                this.clearSession();
                window.location.href = '/index.html?msg=expired';
                return null;
            }

            return {
                ok: response.ok,
                status: response.status,
                data
            };
        } catch (error) {
            console.error('API Error:', error);
            return {
                ok: false,
                status: 0,
                data: { success: false, message: 'Could not connect to backend server: ' + error.message }
            };
        }
    },

    login(username, password) {
        return this.request('/api/login', {
            method: 'POST',
            body: JSON.stringify({ username, password })
        });
    },

    logout() {
        this.clearSession();
        return this.request('/api/logout', { method: 'POST' });
    },

    getLoanTypes() {
        return this.request('/api/loan-types');
    },

    calculateEligibility(params) {
        return this.request('/api/eligibility/calculate', {
            method: 'POST',
            body: JSON.stringify(params)
        });
    },

    submitApplication(params) {
        return this.request('/api/applications', {
            method: 'POST',
            body: JSON.stringify(params)
        });
    },

    getApplications(status = 'ALL', search = '') {
        const query = new URLSearchParams();
        if (status && status !== 'ALL') query.append('status', status);
        if (search) query.append('search', search);
        return this.request(`/api/applications?${query.toString()}`);
    },

    getApplicationDetails(applicationId) {
        return this.request(`/api/applications/details?id=${encodeURIComponent(applicationId)}`);
    },

    getDashboardStats() {
        return this.request('/api/dashboard');
    },

    executeSqlDemo(sql) {
        return this.request('/api/sql-demo', {
            method: 'POST',
            body: JSON.stringify({ sql })
        });
    },

    testJdbc() {
        return this.request('/api/test-jdbc');
    },

    testOdbc() {
        return this.request('/api/test-odbc');
    }
};

/**
 * Initializes the simple student navbar on each page.
 */
function initStudentNavbar(activePage) {
    if (activePage !== 'login' && !API.isAuthenticated()) {
        window.location.href = '/index.html?msg=required';
        return;
    }

    const navArea = document.getElementById('userNavArea');
    if (navArea) {
        const user = API.getUser();
        const roleLabel = user?.role === 'OFFICER' ? 'Loan Officer' : 'Administrator';
        const roleBadgeClass = user?.role === 'OFFICER' ? 'badge-blue' : 'badge-yellow';
        navArea.innerHTML = `
            <div style="display: flex; align-items: center; gap: 0.65rem;">
                <span class="user-badge">${escapeHtml(user?.username || 'User')}</span>
                <span class="badge ${roleBadgeClass}" style="font-size: 0.7rem; padding: 0.15rem 0.45rem;">${roleLabel}</span>
                <button class="btn-logout" id="btnLogout">Logout</button>
            </div>
        `;

        document.getElementById('btnLogout')?.addEventListener('click', async () => {
            await API.logout();
            window.location.href = '/index.html?msg=logged_out';
        });
    }

    if (activePage) {
        const activeLink = document.querySelector(`[data-nav="${activePage}"]`);
        if (activeLink) activeLink.classList.add('active');
    }
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function formatCurrency(num) {
    if (num === null || num === undefined || isNaN(num)) return '₹0';
    return '₹' + Number(num).toLocaleString('en-IN', {
        maximumFractionDigits: 2
    });
}
