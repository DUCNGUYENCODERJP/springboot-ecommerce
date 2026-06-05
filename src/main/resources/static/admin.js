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
            this.showToast("Đăng nhập thành công!");
            this.checkAuth();
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
            return;
        }
        try {
            const profile = await this.api("/api/users/me");
            document.getElementById('admin-name').textContent = profile.fullName;
            document.getElementById('admin-email').textContent = profile.email;
            document.getElementById('admin-initial').textContent = profile.fullName.charAt(0).toUpperCase();

            this.els.authView.classList.add('hidden');
            this.els.dashboardView.style.display = 'flex';
            
            // Initial load of global data
            this.state.hotels = await this.api("/api/hotels").catch(()=>[]);
            
            this.navigate('dashboard');
        } catch (e) {
            this.setToken("");
            this.els.authView.classList.remove('hidden');
            this.els.dashboardView.style.display = 'none';
        }
    },

    // ------------------ DASHBOARD ------------------
    async loadDashboardData() {
        try {
            const [hotels, rooms, bookings, users] = await Promise.all([
                this.api("/api/hotels"),
                this.api("/api/rooms"),
                this.api("/api/bookings"),
                this.api("/api/users")
            ]);
            
            this.state.hotels = hotels;
            this.els.statHotels.textContent = hotels.length;
            this.els.statRooms.textContent = rooms.length;
            this.els.statUsers.textContent = users ? users.length : 0; 

            let totalRevenue = 0;
            const recentBookings = [...bookings].reverse().slice(0, 5);
            
            bookings.forEach(b => {
                if (b.status !== 'CANCELLED') totalRevenue += b.totalPrice;
            });
            this.els.statRevenue.textContent = this.formatMoney(totalRevenue);

            this.renderRecentBookings(recentBookings);
            this.renderFeaturedHotels(hotels.slice(0, 3));
            this.initCharts(bookings, hotels);
        } catch (e) {
            console.error("Failed to load dashboard data", e);
        }
    },

    renderRecentBookings(bookings) {
        if (!bookings.length) {
            this.els.recentBookings.innerHTML = `<tr><td colspan="5" style="text-align:center">Chưa có đặt phòng nào</td></tr>`;
            return;
        }
        this.els.recentBookings.innerHTML = bookings.map(b => `
            <tr>
                <td style="font-weight: 500">${b.bookingCode || 'N/A'}</td>
                <td>${b.guestCount || 0} khách</td>
                <td>${b.room?.hotel?.name || 'Không rõ'}</td>
                <td style="font-weight: 600">${this.formatFullMoney(b.totalPrice || 0)}</td>
                <td>${this.getStatusBadge(b.status || 'CONFIRMED')}</td>
            </tr>
        `).join('');
    },

    getStatusBadge(status) {
        if (status === 'CONFIRMED') return '<span class="badge success">Đã xác nhận</span>';
        if (status === 'PENDING') return '<span class="badge warning">Chờ duyệt</span>';
        if (status === 'CANCELLED') return '<span class="badge danger">Đã hủy</span>';
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
        this.els.tableBookings.innerHTML = this.state.bookings.map(b => `
            <tr>
                <td style="font-weight:500">${b.bookingCode || 'N/A'}</td>
                <td>${b.room?.hotel?.name || 'N/A'}</td>
                <td>Phòng ${b.room?.roomNumber || 'N/A'}</td>
                <td style="font-size:0.85rem">${b.checkInDate} <br> ${b.checkOutDate}</td>
                <td style="font-weight:600">${this.formatFullMoney(b.totalPrice)}</td>
                <td>${this.getStatusBadge(b.status || 'CONFIRMED')}</td>
            </tr>
        `).join('');
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
    initCharts(bookings, hotels) {
        // Line Chart (Revenue)
        const ctxRev = document.getElementById('revenueChart').getContext('2d');
        if(window.revenueChartInstance) window.revenueChartInstance.destroy();
        window.revenueChartInstance = new Chart(ctxRev, {
            type: 'line',
            data: {
                labels: ['T1', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'T8', 'T9', 'T10', 'T11', 'T12'],
                datasets: [
                    {
                        label: 'Năm nay',
                        data: [40, 20, 30, 60, 40, 80, 100, 120, 90, 110, 150, 130],
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
                        data: [80, 72, 80, 20, 28, 20, 40, 60, 50, 70, 90, 85],
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
                responsive: true, maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: { 
                    y: { beginAtZero: true, ticks: { stepSize: 20, color: '#94a3b8' }, grid: { borderDash: [4, 4], color: '#f1f5f9', drawBorder: false } }, 
                    x: { ticks: { color: '#94a3b8' }, grid: { display: false, drawBorder: false } } 
                }
            }
        });

        // Donut Chart (Bookings per hotel)
        // Group bookings by hotel name
        const bookingCounts = {};
        bookings.forEach(b => {
            const hName = b.room?.hotel?.name || 'Khác';
            bookingCounts[hName] = (bookingCounts[hName] || 0) + 1;
        });

        // Sort and pick top 4
        const sortedHotels = Object.entries(bookingCounts).sort((a,b) => b[1] - a[1]);
        const topHotels = sortedHotels.slice(0, 4);
        
        const hotelLabels = topHotels.length ? topHotels.map(h => h[0]) : ['Chưa có dữ liệu'];
        const dataValues = topHotels.length ? topHotels.map(h => h[1]) : [1];
        const totalBookings = dataValues.reduce((a, b) => a + b, 0);
        
        document.getElementById('total-bookings-donut').textContent = topHotels.length ? totalBookings : 0;
        
        const bgColors = ['#2dd4bf', '#f43f5e', '#a855f7', '#fbbf24'];
        
        const ctxHotel = document.getElementById('hotelChart').getContext('2d');
        if(window.hotelChartInstance) window.hotelChartInstance.destroy();
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
                responsive: true, maintainAspectRatio: false,
                cutout: '75%',
                plugins: { legend: { display: false } }
            }
        });

        // Generate Custom Legend
        const legendContainer = document.getElementById('donut-legend');
        legendContainer.innerHTML = dataValues.map((val, i) => {
            const pct = topHotels.length ? Math.round((val / totalBookings) * 100) : 0;
            return `
            <div class="legend-item" style="flex: 1; min-width: 0;">
                <span class="legend-pct" style="color: ${bgColors[i] || '#ccc'}">${pct}%</span>
                <span class="legend-label" style="display: block; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;" title="${hotelLabels[i]}">${hotelLabels[i]}</span>
            </div>
        `}).join('');
    }
};

document.addEventListener('DOMContentLoaded', () => adminApp.init());
