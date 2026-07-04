const adminApp = {
    token: localStorage.getItem("guestToken") || localStorage.getItem("adminToken") || "",
    
    state: {
        hotels: [],
        rooms: [],
        bookings: [],
        users: []
    },

    els: {
        authView: document.getElementById('auth-view'),
        dashboardView: document.getElementById('dashboard-view'),
        loginForm: document.getElementById('admin-login-form'),
        toast: document.getElementById('toast'),
        toastMsg: document.getElementById('toast-msg'),
        
        // Navigation & Views
        navItems: document.querySelectorAll('.nav-item[data-view]'),
        adminViews: document.querySelectorAll('.admin-view'),
        topbarTitle: document.getElementById('topbar-title'),
        
        // Stats
        statBookingsCount: document.getElementById('stat-bookings-count'),
        statHotels: document.getElementById('stat-hotels'),
        statRooms: document.getElementById('stat-rooms'),
        statUsers: document.getElementById('stat-users'),
        statRevenue: document.getElementById('stat-revenue'),
        
        // Tables & Lists
        recentBookings: document.getElementById('recent-bookings-list'),
        featuredHotels: document.getElementById('featured-hotels-list'),
        tableHotels: document.getElementById('table-hotels-body'),
        tableRooms: document.getElementById('table-rooms-body'),
        tableBookings: document.getElementById('table-bookings-body'),
        tableUsers: document.getElementById('table-users-body'),
        hotelRevenueTable: document.getElementById('hotel-revenue-table-body'),
        
        // Modals
        hotelModal: document.getElementById('hotelModal'),
        roomModal: document.getElementById('roomModal'),
        hotelForm: document.getElementById('hotel-form'),
        roomForm: document.getElementById('room-form'),
        roomHotelSelect: document.getElementById('room-hotelId')
    },

    init() {
        this.bindEvents();
        this.checkAuth();
    },

    bindEvents() {
        this.els.loginForm.addEventListener('submit', (e) => this.handleLogin(e));
        this.els.hotelForm.addEventListener('submit', (e) => this.saveHotel(e));
        this.els.roomForm.addEventListener('submit', (e) => this.saveRoom(e));
    },

    navigate(viewId) {
        // Update nav styling
        this.els.navItems.forEach(item => {
            if (item.dataset.view === viewId) item.classList.add('active');
            else item.classList.remove('active');
        });

        // Update views
        this.els.adminViews.forEach(view => {
            if (view.id === `view-${viewId}`) {
                view.classList.remove('hidden');
                view.classList.add('active');
            } else {
                view.classList.add('hidden');
                view.classList.remove('active');
            }
        });

        // Update Breadcrumb
        const titleMap = {
            'dashboard': 'Dashboard',
            'hotels': 'Khách sạn',
            'rooms': 'Phòng',
            'bookings': 'Đặt phòng',
            'users': 'Người dùng'
        };
        this.els.topbarTitle.innerHTML = `Trang chủ > <span>${titleMap[viewId]}</span>`;

        // Load specific data if needed
        if (viewId === 'dashboard') this.loadDashboardData();
        if (viewId === 'hotels') this.loadHotels();
        if (viewId === 'rooms') this.loadRooms();
        if (viewId === 'bookings') this.loadBookings();
        if (viewId === 'users') this.loadUsers();
    },

    openModal(modalId, data = null) {
        const modal = document.getElementById(modalId);
        modal.classList.remove('hidden');
        
        if (modalId === 'hotelModal') {
            document.getElementById('hotel-modal-title').textContent = data ? "Sửa Khách sạn" : "Thêm Khách sạn mới";
            this.els.hotelForm.reset();
            document.getElementById('hotel-id').value = "";
            if (data) {
                for (const key in data) {
                    const input = this.els.hotelForm.elements[key];
                    if (input) input.value = data[key];
                }
            }
        }
        
        if (modalId === 'roomModal') {
            document.getElementById('room-modal-title').textContent = data ? "Sửa Phòng" : "Thêm Phòng mới";
            this.els.roomForm.reset();
            document.getElementById('room-id').value = "";
            
            // Populate hotel options
            this.els.roomHotelSelect.innerHTML = this.state.hotels.map(h => 
                `<option value="${h.id}">${h.name} (${h.city})</option>`
            ).join('');
            
            if (data) {
                for (const key in data) {
                    const input = this.els.roomForm.elements[key];
                    if (input) {
                        if (input.type === 'checkbox') input.checked = data[key];
                        else input.value = data[key];
                    }
                }
                if (data.hotel && data.hotel.id) {
                    this.els.roomHotelSelect.value = data.hotel.id;
                }
            }
        }
    },

    closeModal(modalId) {
        document.getElementById(modalId).classList.add('hidden');
    },

    showToast(message, isError = false) {
        this.els.toastMsg.textContent = message;
        this.els.toast.style.background = isError ? '#ef4444' : '#10b981';
        this.els.toast.classList.add('show');
        setTimeout(() => this.els.toast.classList.remove('show'), 3000);
    },

    formatMoney(amount) {
        if (amount >= 1000000000) return (amount / 1000000000).toFixed(2) + ' tỷ';
        if (amount >= 1000000) return (amount / 1000000).toFixed(2) + ' tr';
        return new Intl.NumberFormat('vi-VN').format(amount) + ' ₫';
    },
    
    formatFullMoney(amount) {
        return new Intl.NumberFormat('vi-VN').format(amount) + ' ₫';
    },

    setToken(token) {
        this.token = token || "";
        if (token) {
            localStorage.setItem("guestToken", token);
            localStorage.setItem("adminToken", token);
        } else {
            localStorage.removeItem("guestToken");
            localStorage.removeItem("adminToken");
        }
    },

    async api(path, options = {}) {
        const headers = { ...(options.headers || {}) };
        if (options.body && !headers["Content-Type"]) headers["Content-Type"] = "application/json";
        if (this.token) headers.Authorization = `Bearer ${this.token}`;
        
        const response = await fetch(path, { ...options, headers });
        if (response.status === 204) return null; // No content
        
        const text = await response.text();
        let data = null;
        try { data = text ? JSON.parse(text) : null; } catch { data = text; }
        
        if (!response.ok) {
            if (data?.errors) {
                const errorMsg = Object.values(data.errors).join(", ");
                throw new Error(errorMsg);
            }
            throw new Error(data?.message || "Lỗi thao tác API");
        }
        return data;
    },

    async handleLogin(e) {
        e.preventDefault();
        const payload = Object.fromEntries(new FormData(e.target).entries());
        try {
            const res = await this.api("/api/auth/login", { method: "POST", body: JSON.stringify(payload) });
            this.setToken(res.accessToken);
            const isAdmin = await this.checkAuth();
            if (isAdmin) {
                this.showToast("Đăng nhập thành công!");
            }
        } catch (error) {
            this.showToast("Thông tin đăng nhập không đúng", true);
        }
    },

    logout() {
        if(confirm("Bạn có chắc chắn muốn đăng xuất?")) {
            this.setToken("");
            this.checkAuth();
        }
    },

    async checkAuth() {
        if (!this.token) {
            this.els.authView.classList.remove('hidden');
            this.els.dashboardView.style.display = 'none';
            return false;
        }
        try {
            const profile = await this.api("/api/users/me");
            if (!profile.roles?.includes('ADMIN')) {
                this.setToken("");
                this.els.authView.classList.remove('hidden');
                this.els.dashboardView.style.display = 'none';
                this.showToast("Tài khoản này không có quyền truy cập dashboard", true);
                return false;
            }
            document.getElementById('admin-name').textContent = profile.fullName;
            document.getElementById('admin-email').textContent = profile.email;
            document.getElementById('admin-initial').textContent = profile.fullName.charAt(0).toUpperCase();

            this.els.authView.classList.add('hidden');
            this.els.dashboardView.style.display = 'flex';
            
            // Initial load of global data
            this.state.hotels = await this.api("/api/hotels").catch(()=>[]);
            
            this.navigate('dashboard');
            return true;
        } catch (e) {
            this.setToken("");
            this.els.authView.classList.remove('hidden');
            this.els.dashboardView.style.display = 'none';
            return false;
        }
    },

    // ------------------ DASHBOARD ------------------
    async loadDashboardData() {
        try {
            const dashboard = await this.api("/api/admin/dashboard");
            const { summary, revenueChart, hotelShare, recentBookings, featuredHotels, hotelRevenueStats } = dashboard;

            this.els.statBookingsCount.textContent = summary.bookingsThisMonth ?? 0;
            this.els.statHotels.textContent = summary.totalHotels ?? 0;
            this.els.statUsers.textContent = summary.totalUsers ?? 0;
            this.els.statRevenue.textContent = this.formatMoney(summary.totalRevenue ?? 0);

            this.updateTrendBadge('booking-growth', summary.bookingGrowthPercent, '#f43f5e');
            this.updateTrendBadge('hotel-growth', summary.hotelGrowthPercent, '#eab308');
            this.updateTrendBadge('revenue-growth', summary.revenueGrowthPercent, '#3b82f6');
            this.updateTrendBadge('user-growth', summary.userGrowthPercent, '#10b981');

            this.renderRecentBookings(recentBookings || []);
            this.renderFeaturedHotels(featuredHotels || []);
            this.initCharts(revenueChart, hotelShare || []);
            
            this.renderHotelRevenueTable(hotelRevenueStats || []);
            this.initHotelRevenueChart(hotelRevenueStats || []);
        } catch (e) {
            console.error("Failed to load dashboard data", e);
            this.showToast("Không tải được dữ liệu dashboard", true);
        }
    },

    updateTrendBadge(elementId, percent, color) {
        const badge = document.getElementById(elementId);
        if (!badge) return;

        const value = Number(percent || 0);
        badge.textContent = `${value >= 0 ? '↗' : '↘'} ${Math.abs(value).toFixed(0)}%`;
        badge.style.color = color;
    },

    renderRecentBookings(bookings) {
        if (!bookings.length) {
            this.els.recentBookings.innerHTML = `<tr><td colspan="5" style="text-align:center">Chưa có đặt phòng nào</td></tr>`;
            return;
        }
        this.els.recentBookings.innerHTML = bookings.map(b => {
            const dateStr = b.createdAt ? new Date(b.createdAt).toLocaleDateString('vi-VN', {day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit'}) : 'N/A';
            return `
            <tr>
                <td style="font-weight: 500">${b.bookingCode || 'N/A'}</td>
                <td>${dateStr}</td>
                <td>${b.user?.fullName || 'Khách ẩn danh'}</td>
                <td>${b.room?.hotel?.name || 'Không rõ'}</td>
                <td style="font-weight: 600">${this.formatFullMoney(b.totalPrice || 0)}</td>
                <td>${this.getStatusBadge(b.status || 'CONFIRMED')}</td>
            </tr>
            `;
        }).join('');
    },

    getStatusBadge(status) {
        if (status === 'CONFIRMED') return '<span class="badge success">Đã xác nhận</span>';
        if (status === 'PENDING') return '<span class="badge warning">Chờ duyệt</span>';
        if (status === 'CANCELLED') return '<span class="badge danger">Đã hủy</span>';
        if (status === 'AVAILABLE') return '<span class="badge success">Sẵn sàng</span>';
        if (status === 'OCCUPIED') return '<span class="badge warning">Đang thuê</span>';
        if (status === 'MAINTENANCE') return '<span class="badge danger">Bảo trì</span>';
        if (status === 'INACTIVE') return '<span class="badge info">Ngừng HĐ</span>';
        return `<span class="badge info">${status}</span>`;
    },

    renderFeaturedHotels(hotels) {
        if (!hotels.length) {
            this.els.featuredHotels.innerHTML = '<div style="padding: 20px; text-align:center; color: #64748b;">Chưa có khách sạn</div>';
            return;
        }
        const img = "https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=150&q=80";
        this.els.featuredHotels.innerHTML = hotels.map(h => `
            <div class="hotel-list-item">
                <img src="${img}" alt="Hotel">
                <div class="hotel-list-info">
                    <h4>${h.name}</h4>
                    <p>${h.address}, ${h.city}</p>
                </div>
                <div class="hotel-stars">⭐ ${h.starRating}.0</div>
            </div>
        `).join('');
    },

    // ------------------ HOTELS ------------------
    async loadHotels() {
        try {
            this.state.hotels = await this.api("/api/hotels");
            this.renderHotels();
        } catch(e) { this.showToast(e.message, true); }
    },
    
    renderHotels() {
        this.els.tableHotels.innerHTML = this.state.hotels.map(h => `
            <tr>
                <td>#${h.id}</td>
                <td style="font-weight:500">${h.name}</td>
                <td>${h.city}, ${h.country}</td>
                <td>⭐ ${h.starRating}</td>
                <td>
                    <button class="btn-action" title="Sửa" onclick='adminApp.openModal("hotelModal", ${JSON.stringify(h)})'>✏️</button>
                    <button class="btn-action" title="Xóa" onclick="adminApp.deleteHotel(${h.id})">🗑️</button>
                </td>
            </tr>
        `).join('');
    },

    async saveHotel(e) {
        e.preventDefault();
        const formData = new FormData(e.target);
        const id = formData.get('id');
        const payload = Object.fromEntries(formData.entries());
        payload.starRating = Number(payload.starRating);
        
        try {
            if (id) {
                await this.api(`/api/hotels/${id}`, { method: "PUT", body: JSON.stringify(payload) });
                this.showToast("Cập nhật khách sạn thành công!");
            } else {
                await this.api("/api/hotels", { method: "POST", body: JSON.stringify(payload) });
                this.showToast("Thêm mới khách sạn thành công!");
            }
            this.closeModal('hotelModal');
            this.loadHotels();
        } catch(err) { this.showToast(err.message, true); }
    },

    async deleteHotel(id) {
        if(!confirm("Bạn có chắc chắn muốn xóa khách sạn này? Toàn bộ phòng sẽ bị xóa theo!")) return;
        try {
            await this.api(`/api/hotels/${id}`, { method: "DELETE" });
            this.showToast("Đã xóa khách sạn");
            this.loadHotels();
        } catch(e) { this.showToast(e.message, true); }
    },

    // ------------------ ROOMS ------------------
    async loadRooms() {
        try {
            this.state.rooms = await this.api("/api/rooms");
            if (!this.state.hotels.length) this.state.hotels = await this.api("/api/hotels");
            this.renderRooms();
        } catch(e) { this.showToast(e.message, true); }
    },
    
    renderRooms() {
        this.els.tableRooms.innerHTML = this.state.rooms.map(r => `
            <tr>
                <td>#${r.id}</td>
                <td>${r.hotel?.name || 'N/A'}</td>
                <td style="font-weight:500">${r.roomNumber}</td>
                <td>${r.type}</td>
                <td>${this.formatFullMoney(r.pricePerNight)}</td>
                <td>${this.getStatusBadge(r.status || 'AVAILABLE')}</td>
                <td>
                    <button class="btn-action" title="Sửa" onclick='adminApp.openModal("roomModal", ${JSON.stringify(r)})'>✏️</button>
                    <button class="btn-action" title="Xóa" onclick="adminApp.deleteRoom(${r.id})">🗑️</button>
                </td>
            </tr>
        `).join('');
    },

    async saveRoom(e) {
        e.preventDefault();
        const formData = new FormData(e.target);
        const id = formData.get('id');
        const payload = Object.fromEntries(formData.entries());
        payload.hotelId = Number(payload.hotelId);
        payload.pricePerNight = Number(payload.pricePerNight);
        payload.capacity = Number(payload.capacity);
        payload.floor = Number(payload.floor);
        payload.hasWifi = formData.get('hasWifi') === 'true' || formData.get('hasWifi') === 'on';
        payload.hasBreakfast = formData.get('hasBreakfast') === 'true' || formData.get('hasBreakfast') === 'on';
        
        try {
            if (id) {
                await this.api(`/api/rooms/${id}`, { method: "PUT", body: JSON.stringify(payload) });
                this.showToast("Cập nhật phòng thành công!");
            } else {
                await this.api("/api/rooms", { method: "POST", body: JSON.stringify(payload) });
                this.showToast("Thêm mới phòng thành công!");
            }
            this.closeModal('roomModal');
            this.loadRooms();
        } catch(err) { this.showToast(err.message, true); }
    },

    async deleteRoom(id) {
        if(!confirm("Bạn có chắc chắn muốn xóa phòng này?")) return;
        try {
            await this.api(`/api/rooms/${id}`, { method: "DELETE" });
            this.showToast("Đã xóa phòng");
            this.loadRooms();
        } catch(e) { this.showToast(e.message, true); }
    },

    // ------------------ BOOKINGS ------------------
    async loadBookings() {
        try {
            this.state.bookings = await this.api("/api/bookings");
            this.renderBookings();
        } catch(e) { this.showToast(e.message, true); }
    },
    
    renderBookings() {
        this.els.tableBookings.innerHTML = this.state.bookings.map(b => {
            let actionHtml = '';
            if (b.status === 'PENDING' || b.status === 'CONFIRMED') {
                actionHtml += `<button class="btn-action" title="Nhận phòng" onclick="adminApp.updateBookingStatus(${b.id}, 'CHECKED_IN')">✅ In</button> `;
                actionHtml += `<button class="btn-action" title="Hủy đơn" onclick="adminApp.updateBookingStatus(${b.id}, 'CANCELLED')">❌</button>`;
            } else if (b.status === 'CHECKED_IN') {
                actionHtml += `<button class="btn-action" title="Trả phòng" onclick="adminApp.updateBookingStatus(${b.id}, 'CHECKED_OUT')">🚪 Out</button>`;
            } else {
                actionHtml = `<span style="color:var(--text-muted); font-size:0.85rem">Không có</span>`;
            }

            const dateStr = b.createdAt ? new Date(b.createdAt).toLocaleDateString('vi-VN', {day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit'}) : 'N/A';
            return `
            <tr>
                <td style="font-weight:500">${b.bookingCode || 'N/A'}</td>
                <td>${dateStr}</td>
                <td>${b.room?.hotel?.name || 'N/A'}</td>
                <td>Phòng ${b.room?.roomNumber || 'N/A'}</td>
                <td style="font-size:0.85rem">${b.checkInDate} <br> ${b.checkOutDate}</td>
                <td style="font-weight:600">${this.formatFullMoney(b.totalPrice)}</td>
                <td>${this.getStatusBadge(b.status || 'CONFIRMED')}</td>
                <td>${actionHtml}</td>
            </tr>
            `;
        }).join('');
    },

    async updateBookingStatus(id, newStatus) {
        if (!confirm(`Bạn có chắc muốn chuyển trạng thái đặt phòng thành ${newStatus}?`)) return;
        try {
            await this.api(`/api/bookings/${id}/status?status=${newStatus}`, { method: "PATCH" });
            this.showToast(`Cập nhật trạng thái thành ${newStatus} thành công!`);
            this.loadBookings();
            // Refresh dashboard data too just in case
            this.loadDashboardData();
        } catch (e) {
            this.showToast(e.message, true);
        }
    },

    // ------------------ USERS ------------------
    async loadUsers() {
        try {
            this.state.users = await this.api("/api/users");
            this.renderUsers();
        } catch(e) { this.showToast(e.message, true); }
    },

    renderUsers() {
        this.els.tableUsers.innerHTML = this.state.users.map(u => `
            <tr>
                <td>#${u.id}</td>
                <td style="font-weight:500">${u.fullName}</td>
                <td>${u.email}</td>
                <td>${u.phone}</td>
                <td>${u.roles?.includes('ADMIN') ? '<span class="badge purple">ADMIN</span>' : '<span class="badge info">USER</span>'}</td>
            </tr>
        `).join('');
    },

    // ------------------ CHARTS ------------------
    initCharts(revenueChart, hotelShare) {
        if (typeof Chart === 'undefined') {
            console.warn("Chart.js is not available; skipping dashboard charts.");
            return;
        }

        const ctxRev = document.getElementById('revenueChart').getContext('2d');
        if (window.revenueChartInstance) window.revenueChartInstance.destroy();
        window.revenueChartInstance = new Chart(ctxRev, {
            type: 'line',
            data: {
                labels: revenueChart?.labels || ['T1', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'T8', 'T9', 'T10', 'T11', 'T12'],
                datasets: [
                    {
                        label: 'Năm nay',
                        data: revenueChart?.currentYear || Array(12).fill(0),
                        borderColor: '#f43f5e',
                        backgroundColor: 'transparent',
                        borderWidth: 2,
                        pointBackgroundColor: '#f43f5e',
                        pointBorderColor: '#fff',
                        pointBorderWidth: 2,
                        pointRadius: 4,
                        tension: 0
                    },
                    {
                        label: 'Năm trước',
                        data: revenueChart?.previousYear || Array(12).fill(0),
                        borderColor: '#2dd4bf',
                        backgroundColor: 'transparent',
                        borderWidth: 2,
                        pointBackgroundColor: '#2dd4bf',
                        pointBorderColor: '#fff',
                        pointBorderWidth: 2,
                        pointRadius: 4,
                        tension: 0
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: { stepSize: 20, color: '#94a3b8' },
                        grid: { borderDash: [4, 4], color: '#f1f5f9', drawBorder: false }
                    },
                    x: {
                        ticks: { color: '#94a3b8' },
                        grid: { display: false, drawBorder: false }
                    }
                }
            }
        });

        const topHotels = (hotelShare || []).slice(0, 4);
        const hotelLabels = topHotels.length ? topHotels.map(h => h.hotelName) : ['Chưa có dữ liệu'];
        const dataValues = topHotels.length ? topHotels.map(h => h.bookingCount) : [1];
        const totalBookings = topHotels.length ? dataValues.reduce((a, b) => a + b, 0) : 0;

        document.getElementById('total-bookings-donut').textContent = totalBookings;

        const bgColors = ['#2dd4bf', '#f43f5e', '#a855f7', '#fbbf24'];

        const ctxHotel = document.getElementById('hotelChart').getContext('2d');
        if (window.hotelChartInstance) window.hotelChartInstance.destroy();
        window.hotelChartInstance = new Chart(ctxHotel, {
            type: 'doughnut',
            data: {
                labels: hotelLabels,
                datasets: [{
                    data: dataValues,
                    backgroundColor: bgColors,
                    borderWidth: 4,
                    borderColor: '#ffffff',
                    hoverOffset: 4
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                cutout: '75%',
                plugins: { legend: { display: false } }
            }
        });

        const legendContainer = document.getElementById('donut-legend');
        legendContainer.innerHTML = dataValues.map((val, i) => {
            const pct = totalBookings ? Math.round((val / totalBookings) * 100) : 0;
            return `
            <div class="legend-item" style="flex: 1; min-width: 0;">
                <span class="legend-pct" style="color: ${bgColors[i] || '#ccc'}">${pct}%</span>
                <span class="legend-label" style="display: block; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;" title="${hotelLabels[i]}">${hotelLabels[i]}</span>
            </div>
        `}).join('');
    },

    renderHotelRevenueTable(stats) {
        if (!this.els.hotelRevenueTable) return;
        
        if (!stats || !stats.length) {
            this.els.hotelRevenueTable.innerHTML = `<tr><td colspan="7" style="text-align:center">Chưa có dữ liệu</td></tr>`;
            return;
        }
        
        this.els.hotelRevenueTable.innerHTML = stats.map((stat, index) => {
            const rank = index + 1;
            let rankHtml = rank;
            if (rank === 1) rankHtml = `<span class="badge" style="background:#fbbf24;color:#fff;">🥇 1</span>`;
            else if (rank === 2) rankHtml = `<span class="badge" style="background:#94a3b8;color:#fff;">🥈 2</span>`;
            else if (rank === 3) rankHtml = `<span class="badge" style="background:#b45309;color:#fff;">🥉 3</span>`;
            else rankHtml = `<span style="font-weight:600;color:#64748b;padding-left:8px;">${rank}</span>`;
            
            const growth = stat.revenueGrowthPercent || 0;
            const growthHtml = growth >= 0 
                ? `<span style="color:var(--success);font-weight:500;">↗ ${growth}%</span>`
                : `<span style="color:var(--danger);font-weight:500;">↘ ${Math.abs(growth)}%</span>`;

            return `
            <tr>
                <td>${rankHtml}</td>
                <td>
                    <div style="font-weight:600;">${stat.hotelName || 'N/A'}</div>
                    <div style="font-size:0.75rem;color:var(--text-muted);">⭐ ${stat.starRating || 0} sao</div>
                </td>
                <td>${stat.city || 'N/A'}</td>
                <td style="font-weight:600;color:var(--primary);">${this.formatFullMoney(stat.totalRevenue || 0)}</td>
                <td style="font-weight:500;">${this.formatFullMoney(stat.revenueThisMonth || 0)}</td>
                <td>${stat.bookingsThisMonth || 0} / ${stat.totalBookings || 0}</td>
                <td>${growthHtml}</td>
            </tr>
            `;
        }).join('');
    },

    initHotelRevenueChart(stats) {
        if (typeof Chart === 'undefined') return;

        const canvas = document.getElementById('hotelRevenueBarChart');
        if (!canvas) return;
        
        if (window.hotelRevenueChartInstance) {
            window.hotelRevenueChartInstance.destroy();
        }
        
        // Lấy top 5 khách sạn có doanh thu cao nhất để vẽ biểu đồ
        const topStats = (stats || []).slice(0, 5);
        if (topStats.length === 0) return;
        
        const labels = ['T1', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'T8', 'T9', 'T10', 'T11', 'T12'];
        const colors = ['#3b82f6', '#f43f5e', '#10b981', '#f59e0b', '#8b5cf6'];
        
        const datasets = topStats.map((stat, i) => {
            return {
                label: stat.hotelName || `Hotel ${stat.hotelId}`,
                data: stat.monthlyRevenue || Array(12).fill(0),
                backgroundColor: colors[i % colors.length],
                borderRadius: 4,
            };
        });

        window.hotelRevenueChartInstance = new Chart(canvas.getContext('2d'), {
            type: 'bar',
            data: {
                labels: labels,
                datasets: datasets
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { 
                    legend: { 
                        position: 'top',
                        labels: { boxWidth: 12, usePointStyle: true, padding: 20 }
                    },
                    tooltip: {
                        callbacks: {
                            label: function(context) {
                                let label = context.dataset.label || '';
                                if (label) {
                                    label += ': ';
                                }
                                if (context.parsed.y !== null) {
                                    label += new Intl.NumberFormat('vi-VN').format(context.parsed.y) + ' ₫';
                                }
                                return label;
                            }
                        }
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: { 
                            color: '#94a3b8',
                            callback: function(value) {
                                if (value >= 1000000000) return (value / 1000000000) + 'T';
                                if (value >= 1000000) return (value / 1000000) + 'Tr';
                                if (value >= 1000) return (value / 1000) + 'K';
                                return value;
                            }
                        },
                        grid: { borderDash: [4, 4], color: '#f1f5f9', drawBorder: false }
                    },
                    x: {
                        ticks: { color: '#94a3b8' },
                        grid: { display: false, drawBorder: false }
                    }
                }
            }
        });
    }
};

document.addEventListener('DOMContentLoaded', () => adminApp.init());
