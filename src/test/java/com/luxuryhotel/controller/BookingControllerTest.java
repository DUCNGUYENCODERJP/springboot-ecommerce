package com.luxuryhotel.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luxuryhotel.domain.hotel.Hotel;
import com.luxuryhotel.domain.user.Role;
import com.luxuryhotel.domain.user.User;
import com.luxuryhotel.domain.room.RoomStatus;
import com.luxuryhotel.domain.room.RoomType;
import com.luxuryhotel.dto.auth.LoginRequest;
import com.luxuryhotel.dto.auth.RegisterRequest;
import com.luxuryhotel.dto.booking.CreateBookingRequest;
import com.luxuryhotel.dto.room.RoomRequest;
import com.luxuryhotel.repository.BookingRepository;
import com.luxuryhotel.repository.HotelRepository;
import com.luxuryhotel.repository.RoomRepository;
import com.luxuryhotel.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

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
        hotel.setName("Luxury Saigon");
        hotel.setAddress("1 Nguyen Hue");
        hotel.setCity("Ho Chi Minh City");
        hotel.setCountry("Vietnam");
        hotel.setStarRating(5);
        hotel.setDescription("Central luxury hotel");
        hotelId = hotelRepository.save(hotel).getId();
    }

    @Test
    void adminCanCreateRoomAndCustomerCanBookAvailableRoom() throws Exception {
        String adminToken = login("admin@test.com", "Admin@12345");
        Long roomId = createRoom(adminToken, "A101");

        RegisterRequest registerRequest = new RegisterRequest(
                "Customer Booking",
                "booker@example.com",
                "0911222333",
                "Password@123"
        );

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode registerJson = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        String customerToken = registerJson.get("accessToken").asText();

        mockMvc.perform(get("/api/rooms/available")
                        .param("checkInDate", LocalDate.now().plusDays(2).toString())
                        .param("checkOutDate", LocalDate.now().plusDays(4).toString())
                        .param("hotelId", hotelId.toString())
                        .param("guestCount", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].roomNumber").value("A101"));

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
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.room.roomNumber").value("A101"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.totalPrice").value(300.00));

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Room is already booked for the selected date range"));
    }

    @Test
    void customerCanCancelFutureBooking() throws Exception {
        String adminToken = login("admin@test.com", "Admin@12345");
        Long roomId = createRoom(adminToken, "B201");

        RegisterRequest registerRequest = new RegisterRequest(
                "Cancel Customer",
                "cancel@example.com",
                "0944556677",
                "Password@123"
        );

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String customerToken = objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .get("accessToken")
                .asText();

        CreateBookingRequest bookingRequest = new CreateBookingRequest(
                roomId,
                LocalDate.now().plusDays(5),
                LocalDate.now().plusDays(7),
                1,
                null
        );

        MvcResult bookingResult = mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long bookingId = objectMapper.readTree(bookingResult.getResponse().getContentAsString())
                .get("id")
                .asLong();

        mockMvc.perform(patch("/api/bookings/{bookingId}/cancel", bookingId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void adminCannotDeleteRoomWithExistingBookings() throws Exception {
        String adminToken = login("admin@test.com", "Admin@12345");
        Long roomId = createRoom(adminToken, "D401");

        RegisterRequest registerRequest = new RegisterRequest(
                "Delete Guard Customer",
                "delete-guard@example.com",
                "0977665544",
                "Password@123"
        );

        String customerToken = objectMapper.readTree(mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString()).get("accessToken").asText();

        CreateBookingRequest bookingRequest = new CreateBookingRequest(
                roomId,
                LocalDate.now().plusDays(3),
                LocalDate.now().plusDays(5),
                2,
                null
        );

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/rooms/{roomId}", roomId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot delete room with existing bookings"));
    }

    private String login(String email, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(email, password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
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
}
