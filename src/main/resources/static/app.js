const app = {
    state: {
        token: localStorage.getItem("guestToken") || "",
        currentUser: null,
        hotels: [],
        rooms: [],
        currentHotel: null,
        currentRoom: null,
        searchParams: {
            checkIn: '',
            checkOut: '',
            hotelId: ''
        },
        toastTimer: null
    },

    els: {},

    init() {
        this.cacheDOM();
        this.bindEvents();
        this.setDefaultDates();
        this.checkAuth();
        this.loadInitialData();
        
        // Initial view
        this.navigate('home');
    },

    cacheDOM() {
        // Views
        this.els.views = document.querySelectorAll('.view');
        
        // Nav
        this.els.navLogin = document.getElementById('nav-login');
        this.els.navRegister = document.getElementById('nav-register');
        this.els.navUser = document.getElementById('nav-user');
        this.els.userName = document.getElementById('user-name');
        
        // Forms
        this.els.homeSearchForm = document.getElementById('home-search-form');
        this.els.searchPageForm = document.getElementById('search-page-form');
        this.els.loginForm = document.getElementById('login-form');
        this.els.registerForm = document.getElementById('register-form');
        this.els.bookingForm = document.getElementById('booking-form');
        
        // Selects
        this.els.homeHotelSelect = document.getElementById('home-hotel-select');
        this.els.searchHotelSelect = document.getElementById('search-hotel-select');
        
        // Grids
        this.els.featuredHotelsGrid = document.getElementById('featured-hotels-grid');
        this.els.searchResultsGrid = document.getElementById('search-results-grid');
        this.els.searchMeta = document.getElementById('search-meta');
        
        // Detail View
        this.els.detailName = document.getElementById('detail-name');
        this.els.detailAddress = document.getElementById('detail-address');
        this.els.detailDesc = document.getElementById('detail-desc');
        this.els.detailStars = document.getElementById('detail-stars');
        this.els.detailRoomList = document.getElementById('detail-room-list');
        this.els.detailCheckin = document.getElementById('detail-checkin');
        this.els.detailCheckout = document.getElementById('detail-checkout');
        
        // Booking Summary
        this.els.summaryHotel = document.getElementById('summary-hotel');
        this.els.summaryAddress = document.getElementById('summary-address');
        this.els.summaryRoomType = document.getElementById('summary-room-type');
        this.els.summaryCheckinTxt = document.getElementById('summary-checkin-txt');
        this.els.summaryCheckoutTxt = document.getElementById('summary-checkout-txt');
        this.els.summaryNights = document.getElementById('summary-nights');
        this.els.summaryTotalPrice = document.getElementById('summary-total-price');
        
        // Toast
        this.els.toast = document.getElementById('toast');
        this.els.toastMsg = document.getElementById('toast-msg');
    },

    bindEvents() {
        this.els.homeSearchForm.addEventListener('submit', (e) => this.handleSearch(e, 'home'));
        this.els.searchPageForm.addEventListener('submit', (e) => this.handleSearch(e, 'search'));
        this.els.loginForm.addEventListener('submit', (e) => this.handleLogin(e));
        this.els.registerForm.addEventListener('submit', (e) => this.handleRegister(e));
        this.els.bookingForm.addEventListener('submit', (e) => this.handleBooking(e));
    },

    setDefaultDates() {
        const today = new Date();
        const checkIn = new Date(today);
        checkIn.setDate(today.getDate() + 1);
        const checkOut = new Date(today);
        checkOut.setDate(today.getDate() + 3);

        const format = (date) => date.toISOString().slice(0, 10);
        
        this.state.searchParams.checkIn = format(checkIn);
        this.state.searchParams.checkOut = format(checkOut);

        this.els.homeSearchForm.checkInDate.value = this.state.searchParams.checkIn;
        this.els.homeSearchForm.checkOutDate.value = this.state.searchParams.checkOut;
        this.els.searchPageForm.checkInDate.value = this.state.searchParams.checkIn;
        this.els.searchPageForm.checkOutDate.value = this.state.searchParams.checkOut;
    },

    navigate(viewId) {
        window.scrollTo(0, 0);
        this.els.views.forEach(view => {
            view.classList.add('hidden');
        });
        document.getElementById(`view-${viewId}`).classList.remove('hidden');
        
        if (viewId === 'search') {
            this.loadAllRooms();
        }
    },

    showToast(message, isError = false) {
        clearTimeout(this.state.toastTimer);
        this.els.toastMsg.textContent = message;
        this.els.toast.style.background = isError ? '#ef4444' : '#1e293b';
        this.els.toast.classList.add('show');
        this.state.toastTimer = setTimeout(() => this.els.toast.classList.remove('show'), 3000);
    },

    formatMoney(amount) {
        return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
    },

    async api(path, options = {}) {
        const headers = {
            ...(options.headers || {})
        };
        if (options.body && !headers["Content-Type"]) {
            headers["Content-Type"] = "application/json";
        }
        if (this.state.token) {
            headers.Authorization = `Bearer ${this.state.token}`;
        }
        const response = await fetch(path, { ...options, headers });
        const text = await response.text();
        let data = null;
        try { data = text ? JSON.parse(text) : null; } catch { data = text; }
        if (!response.ok) throw new Error(data?.message || "Request failed");
        return data;
    },

    setToken(token) {
        this.state.token = token || "";
        if (token) localStorage.setItem("guestToken", token);
        else localStorage.removeItem("guestToken");
    },

    async checkAuth() {
        if (!this.state.token) return this.updateAuthUI();
        try {
            this.state.currentUser = await this.api("/api/users/me");
            this.updateAuthUI();
        } catch (e) {
            this.setToken("");
            this.state.currentUser = null;
            this.updateAuthUI();
        }
    },

    updateAuthUI() {
        if (this.state.currentUser) {
            this.els.navLogin.classList.add('hidden');
            this.els.navRegister.classList.add('hidden');
            this.els.navUser.classList.remove('hidden');
            this.els.userName.textContent = `Xin chào, ${this.state.currentUser.fullName}`;
            
            // Show admin link if role is ADMIN
            const adminLink = document.getElementById('nav-admin-link');
            if (adminLink) {
                if (this.state.currentUser.roles && this.state.currentUser.roles.includes('ADMIN')) {
                    adminLink.classList.remove('hidden');
                } else {
                    adminLink.classList.add('hidden');
                }
            }
            
            // Auto fill booking form
            document.getElementById('booking-name').value = this.state.currentUser.fullName;
            document.getElementById('booking-email').value = this.state.currentUser.email;
            document.getElementById('booking-phone').value = this.state.currentUser.phone;
        } else {
            this.els.navLogin.classList.remove('hidden');
            this.els.navRegister.classList.remove('hidden');
            this.els.navUser.classList.add('hidden');
            
            const adminLink = document.getElementById('nav-admin-link');
            if (adminLink) adminLink.classList.add('hidden');
            
            document.getElementById('booking-name').value = '';
            document.getElementById('booking-email').value = '';
            document.getElementById('booking-phone').value = '';
        }
    },

    logout() {
        this.setToken("");
        this.state.currentUser = null;
        this.updateAuthUI();
        this.showToast("Đã đăng xuất");
        this.navigate('home');
    },

    async handleLogin(e) {
        e.preventDefault();
        const payload = Object.fromEntries(new FormData(e.target).entries());
        try {
            const res = await this.api("/api/auth/login", { method: "POST", body: JSON.stringify(payload) });
            this.setToken(res.accessToken);
            await this.checkAuth();
            this.showToast("Đăng nhập thành công!");
            this.navigate('home');
            e.target.reset();
        } catch (error) {
            this.showToast("Email hoặc mật khẩu không đúng", true);
        }
    },

    async handleRegister(e) {
        e.preventDefault();
        const payload = Object.fromEntries(new FormData(e.target).entries());
        try {
            const res = await this.api("/api/auth/register", { method: "POST", body: JSON.stringify(payload) });
            this.setToken(res.accessToken);
            await this.checkAuth();
            this.showToast("Đăng ký thành công!");
            this.navigate('home');
            e.target.reset();
        } catch (error) {
            this.showToast(error.message, true);
        }
    },

    async loadInitialData() {
        try {
            this.state.hotels = await this.api("/api/hotels");
            this.renderHotelSelects();
            this.renderFeaturedHotels();
        } catch (e) {
            console.error("Failed to load hotels", e);
        }
    },

    renderHotelSelects() {
        const options = `<option value="">Tất cả địa điểm</option>` + 
            this.state.hotels.map(h => `<option value="${h.id}">${h.city} - ${h.name}</option>`).join('');
        this.els.homeHotelSelect.innerHTML = options;
        this.els.searchHotelSelect.innerHTML = options;
    },

    renderFeaturedHotels() {
        if (!this.state.hotels.length) return;
        const images = [
            "https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=800&q=80",
            "https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?auto=format&fit=crop&w=800&q=80",
            "https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?auto=format&fit=crop&w=800&q=80",
            "https://images.unsplash.com/photo-1445019980597-93fa8acb246c?auto=format&fit=crop&w=800&q=80"
        ];
        
        const featured = this.state.hotels.slice(0, 4);
        this.els.featuredHotelsGrid.innerHTML = featured.map((h, i) => `
            <div class="hotel-card" onclick="app.viewHotel(${h.id})" style="cursor:pointer">
                <div class="hotel-card__image" style="background-image: url('${images[i % images.length]}')">
                    <div class="hotel-card__rating">⭐ ${h.starRating}.0</div>
                </div>
                <div class="hotel-card__content">
                    <h3>${h.name}</h3>
                    <div class="hotel-card__location">📍 ${h.city}, ${h.country}</div>
                    <div class="hotel-card__footer">
                        <span class="text-muted" style="font-size:0.85rem">Xem chi tiết</span>
                        <div class="hotel-card__price">→</div>
                    </div>
                </div>
            </div>
        `).join('');
    },

    async handleSearch(e, source) {
        e.preventDefault();
        const form = e.target;
        this.state.searchParams.hotelId = form.hotelId.value;
        this.state.searchParams.checkIn = form.checkInDate.value;
        this.state.searchParams.checkOut = form.checkOutDate.value;
        
        // Sync forms
        if (source === 'home') {
            this.els.searchHotelSelect.value = this.state.searchParams.hotelId;
            this.els.searchPageForm.checkInDate.value = this.state.searchParams.checkIn;
            this.els.searchPageForm.checkOutDate.value = this.state.searchParams.checkOut;
        } else {
            this.els.homeHotelSelect.value = this.state.searchParams.hotelId;
            this.els.homeSearchForm.checkInDate.value = this.state.searchParams.checkIn;
            this.els.homeSearchForm.checkOutDate.value = this.state.searchParams.checkOut;
        }

        this.navigate('search');
        await this.loadAvailableRooms();
    },

    async loadAllRooms() {
        this.els.searchResultsGrid.innerHTML = '<div class="loading-spinner"></div>';
        try {
            const rooms = await this.api("/api/rooms");
            this.renderSearchResults(rooms, `Tìm thấy ${rooms.length} phòng`);
        } catch (e) {
            this.els.searchResultsGrid.innerHTML = '<div class="empty-state">Lỗi tải dữ liệu</div>';
        }
    },

    async loadAvailableRooms() {
        this.els.searchResultsGrid.innerHTML = '<div class="loading-spinner"></div>';
        const params = new URLSearchParams();
        if (this.state.searchParams.hotelId) params.set('hotelId', this.state.searchParams.hotelId);
        params.set('checkInDate', this.state.searchParams.checkIn);
        params.set('checkOutDate', this.state.searchParams.checkOut);
        
        try {
            const rooms = await this.api(`/api/rooms/available?${params.toString()}`);
            this.renderSearchResults(rooms, `Tìm thấy ${rooms.length} phòng trống`);
        } catch (e) {
            this.els.searchResultsGrid.innerHTML = '<div class="empty-state">Lỗi tìm kiếm phòng</div>';
        }
    },

    renderSearchResults(rooms, metaTxt) {
        this.els.searchMeta.textContent = metaTxt;
        if (!rooms.length) {
            this.els.searchResultsGrid.innerHTML = '<div class="empty-state">Không tìm thấy phòng phù hợp. Vui lòng thử ngày khác.</div>';
            return;
        }

        const images = [
            "https://images.unsplash.com/photo-1566665797739-1674de7a421a?auto=format&fit=crop&w=600&q=80",
            "https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=600&q=80",
            "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=600&q=80"
        ];

        // Group by hotel for nicer display if needed, but for now display individual rooms as "hotel cards"
        this.els.searchResultsGrid.innerHTML = rooms.map((room, i) => `
            <div class="hotel-card">
                <div class="hotel-card__image" style="background-image: url('${images[i % images.length]}')">
                    <div class="hotel-card__rating">⭐ ${room.hotel.starRating}.0</div>
                </div>
                <div class="hotel-card__content">
                    <h3>${room.hotel.name}</h3>
                    <div class="hotel-card__location">📍 ${room.hotel.address}, ${room.hotel.city}</div>
                    <p style="font-size:0.9rem; color: var(--text-muted); margin-bottom: 16px;">
                        Phòng ${room.type} • Tối đa ${room.capacity} người
                    </p>
                    <div class="hotel-card__footer">
                        <div class="hotel-card__price">${this.formatMoney(room.pricePerNight)}<span>/đêm</span></div>
                        <button class="btn btn-primary" onclick="app.viewHotel(${room.hotel.id})">Chọn phòng</button>
                    </div>
                </div>
            </div>
        `).join('');
    },

    async viewHotel(hotelId) {
        this.state.currentHotel = this.state.hotels.find(h => h.id === hotelId);
        if (!this.state.currentHotel) return;
        
        const h = this.state.currentHotel;
        this.els.detailName.textContent = h.name;
        this.els.detailAddress.textContent = `${h.address}, ${h.city}, ${h.country}`;
        this.els.detailDesc.textContent = h.description || `${h.name} mang đến trải nghiệm nghỉ dưỡng tuyệt vời tại trung tâm ${h.city}. Khách sạn được trang bị đầy đủ tiện nghi cao cấp, không gian sang trọng và dịch vụ chuyên nghiệp, chắc chắn sẽ làm hài lòng quý khách.`;
        this.els.detailStars.textContent = h.starRating;
        
        this.els.detailCheckin.value = this.state.searchParams.checkIn;
        this.els.detailCheckout.value = this.state.searchParams.checkOut;

        this.navigate('detail');
        this.els.detailRoomList.innerHTML = '<div class="loading-spinner"></div>';
        
        try {
            // Get all rooms for this hotel
            const allRooms = await this.api("/api/rooms");
            const hotelRooms = allRooms.filter(r => r.hotel.id === hotelId);
            this.renderHotelRooms(hotelRooms);
        } catch (e) {
            this.els.detailRoomList.innerHTML = '<div class="empty-state">Không tải được danh sách phòng</div>';
        }
    },

    renderHotelRooms(rooms) {
        if (!rooms.length) {
            this.els.detailRoomList.innerHTML = '<div class="empty-state">Khách sạn chưa có phòng nào.</div>';
            return;
        }

        const roomImg = "https://images.unsplash.com/photo-1566665797739-1674de7a421a?auto=format&fit=crop&w=400&q=80";
        
        this.els.detailRoomList.innerHTML = rooms.map(room => `
            <div class="room-list-item">
                <div class="room-img" style="background-image: url('${roomImg}')"></div>
                <div class="room-info">
                    <h4>Phòng ${room.type} (Số ${room.roomNumber})</h4>
                    <div class="room-features">
                        <span>👥 ${room.capacity} Người</span>
                        <span>📶 ${room.hasWifi ? 'Free Wifi' : 'No Wifi'}</span>
                        <span>🍳 ${room.hasBreakfast ? 'Bao gồm bữa sáng' : 'Không bữa sáng'}</span>
                    </div>
                    <p style="font-size:0.85rem; color:var(--text-muted)">${room.description || 'Phòng nghỉ sang trọng, đầy đủ tiện nghi với view tuyệt đẹp.'}</p>
                </div>
                <div class="room-price-action">
                    <div class="room-price">${this.formatMoney(room.pricePerNight)}</div>
                    <span class="text-muted" style="font-size:0.8rem; display:block; text-align:right">/đêm</span>
                    <button class="btn btn-primary mt-4" style="width:100%" onclick="app.bookRoom(${room.id})">Đặt phòng</button>
                </div>
            </div>
        `).join('');
    },

    bookRoom(roomId) {
        if (!this.state.token) {
            this.showToast("Vui lòng đăng nhập để đặt phòng", true);
            this.navigate('login');
            return;
        }

        // We need to find the room details again or pass it
        // Simpler: fetch room detail or find in state
        const allRooms = [].concat(this.state.rooms); // This might be empty if we navigated directly
        // Let's re-fetch the room to be safe or just find it if we cached it.
        // For simplicity, we can fetch all rooms again or rely on the api. 
        // We know the hotel, we can just make an api call for the specific room. 
        this.fetchAndGoToBooking(roomId);
    },

    async fetchAndGoToBooking(roomId) {
        try {
            const allRooms = await this.api("/api/rooms");
            const room = allRooms.find(r => r.id === roomId);
            if (!room) throw new Error("Room not found");
            
            this.state.currentRoom = room;
            
            // Populate Booking form
            document.getElementById('booking-room-id').value = room.id;
            document.getElementById('booking-checkin').value = this.state.searchParams.checkIn;
            document.getElementById('booking-checkout').value = this.state.searchParams.checkOut;
            
            // Populate summary
            this.els.summaryHotel.textContent = room.hotel.name;
            this.els.summaryAddress.textContent = `${room.hotel.address}, ${room.hotel.city}`;
            this.els.summaryRoomType.textContent = `Phòng ${room.type}`;
            this.els.summaryCheckinTxt.textContent = this.state.searchParams.checkIn;
            this.els.summaryCheckoutTxt.textContent = this.state.searchParams.checkOut;
            
            // Calculate total
            const checkInDate = new Date(this.state.searchParams.checkIn);
            const checkOutDate = new Date(this.state.searchParams.checkOut);
            let nights = Math.ceil((checkOutDate - checkInDate) / (1000 * 60 * 60 * 24));
            if (nights < 1) nights = 1;
            
            this.els.summaryNights.textContent = `${nights} đêm`;
            this.els.summaryTotalPrice.textContent = this.formatMoney(room.pricePerNight * nights);

            this.navigate('booking');
        } catch (e) {
            this.showToast("Lỗi khi tải thông tin phòng", true);
        }
    },

    async handleBooking(e) {
        e.preventDefault();
        const payload = Object.fromEntries(new FormData(e.target).entries());
        payload.roomId = Number(payload.roomId);
        payload.guestCount = Number(payload.guestCount) || 1;
        
        try {
            const booking = await this.api("/api/bookings", {
                method: "POST",
                body: JSON.stringify(payload)
            });
            this.showToast(`Đặt phòng thành công! Mã: ${booking.bookingCode}`);
            this.navigate('home');
        } catch (error) {
            this.showToast(error.message, true);
        }
    }
};

document.addEventListener('DOMContentLoaded', () => app.init());
