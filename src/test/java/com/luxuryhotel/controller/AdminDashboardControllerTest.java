package com.luxuryhotel.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luxuryhotel.domain.hotel.Hotel;
import com.luxuryhotel.domain.room.RoomStatus;
import com.luxuryhotel.domain.room.RoomType;
import com.luxuryhotel.domain.user.Role;
import com.luxuryhotel.domain.user.User;
import com.luxuryhotel.dto.auth.LoginRequest;
import com.luxuryhotel.dto.auth.RegisterRequest;
import com.luxuryhotel.dto.booking.CreateBookingRequest;
import com.luxuryhotel.dto.hotel.HotelRequest;
import com.luxuryhotel.dto.room.RoomRequest;
import com.luxuryhotel.repository.BookingRepository;
import com.luxuryhotel.repository.HotelRepository;
import com.luxuryhotel.repository.RoomRepository;
import com.luxuryhotel.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long hotelId;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        roomRepository.deleteAll();
        hotelRepository.deleteAll();
        userRepository.deleteAll();

        User admin = new User();
        admin.setFullName("Test Admin");
        admin.setEmail("admin@test.com");
        admin.setPhone("0900000000");
        admin.setPassword(passwordEncoder.encode("Admin@12345"));
        admin.setEnabled(true);
        admin.setRoles(Set.of(Role.ADMIN));
        userRepository.save(admin);

        Hotel hotel = new Hotel();
        hotel.setName("Luxury Hanoi");
        hotel.setAddress("123 West Lake");
        hotel.setCity("Hanoi");
        hotel.setCountry("Vietnam");
        hotel.setStarRating(5);
        hotel.setDescription("Premium business hotel");
        hotelId = hotelRepository.save(hotel).getId();
    }

    @AfterEach
    void tearDown() {
        bookingRepository.deleteAll();
        roomRepository.deleteAll();
        hotelRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void adminDashboardReturnsAggregatedMetrics() throws Exception {
        String adminToken = login("admin@test.com", "Admin@12345");
        Long roomId = createRoom(adminToken, "A101");

        String customerToken = registerCustomer("dashboard-customer@test.com", "0911222333");
        createBooking(customerToken, roomId);

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalBookings").value(1))
                .andExpect(jsonPath("$.summary.bookingsThisMonth").value(1))
                .andExpect(jsonPath("$.summary.totalHotels").value(1))
                .andExpect(jsonPath("$.summary.totalUsers").value(2))
                .andExpect(jsonPath("$.recentBookings.length()").value(1))
                .andExpect(jsonPath("$.featuredHotels.length()").value(1))
                .andExpect(jsonPath("$.revenueChart.labels.length()").value(12))
                .andExpect(jsonPath("$.hotelShare.length()").value(1));
    }

    private String login(String email, String password) throws Exception {
        LoginRequest request = new LoginRequest(email, password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken")
                .asText();
    }

    private String registerCustomer(String email, String phone) throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Dashboard Customer",
                email,
                phone,
                "Password@123"
        );

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken")
                .asText();
    }

    private Long createRoom(String adminToken, String roomNumber) throws Exception {
        RoomRequest roomRequest = new RoomRequest(
                hotelId,
                roomNumber,
                RoomType.DELUXE,
                RoomStatus.AVAILABLE,
                new BigDecimal("150.00"),
                2,
                10,
                "Ocean view",
                true,
                true
        );

        MvcResult result = mockMvc.perform(post("/api/rooms")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roomRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id")
                .asLong();
    }

    private void createBooking(String customerToken, Long roomId) throws Exception {
        CreateBookingRequest bookingRequest = new CreateBookingRequest(
                roomId,
                LocalDate.now().plusDays(2),
                LocalDate.now().plusDays(4),
                2,
                "High floor please"
        );

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isCreated());
    }
}
