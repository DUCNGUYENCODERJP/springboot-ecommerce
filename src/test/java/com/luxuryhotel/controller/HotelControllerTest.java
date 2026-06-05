package com.luxuryhotel.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luxuryhotel.domain.user.Role;
import com.luxuryhotel.domain.user.User;
import com.luxuryhotel.dto.auth.LoginRequest;
import com.luxuryhotel.dto.hotel.HotelRequest;
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

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HotelControllerTest {

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
    }

    @Test
    void adminCanCreateHotelAndPublicCanListHotels() throws Exception {
        String token = login("admin@test.com", "Admin@12345");
        HotelRequest request = new HotelRequest(
                "Luxury Hanoi",
                "123 West Lake",
                "Hanoi",
                "Vietnam",
                5,
                "Premium business hotel"
        );

        mockMvc.perform(post("/api/hotels")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Luxury Hanoi"))
                .andExpect(jsonPath("$.city").value("Hanoi"));

        mockMvc.perform(get("/api/hotels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Luxury Hanoi"));
    }

    @Test
    void adminCannotDeleteHotelWhileRoomsStillExist() throws Exception {
        String token = login("admin@test.com", "Admin@12345");
        HotelRequest hotelRequest = new HotelRequest(
                "Luxury Hanoi",
                "123 West Lake",
                "Hanoi",
                "Vietnam",
                5,
                "Premium business hotel"
        );

        MvcResult hotelResult = mockMvc.perform(post("/api/hotels")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hotelRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        Long hotelId = objectMapper.readTree(hotelResult.getResponse().getContentAsString())
                .get("id")
                .asLong();

        String roomJson = """
                {
                  "hotelId": %d,
                  "roomNumber": "C301",
                  "type": "DELUXE",
                  "status": "AVAILABLE",
                  "pricePerNight": 180.00,
                  "capacity": 2,
                  "floor": 3,
                  "description": "Corner room",
                  "hasWifi": true,
                  "hasBreakfast": true
                }
                """.formatted(hotelId);

        mockMvc.perform(post("/api/rooms")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(roomJson))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/hotels/{hotelId}", hotelId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot delete hotel while rooms still exist"));
    }

    @Test
    void publicCanReadHotelsWithPaginationEndpoint() throws Exception {
        String token = login("admin@test.com", "Admin@12345");
        for (int i = 1; i <= 3; i++) {
            HotelRequest request = new HotelRequest(
                    "Luxury Hanoi " + i,
                    "123 West Lake " + i,
                    "Hanoi",
                    "Vietnam",
                    5,
                    "Premium business hotel " + i
            );

            mockMvc.perform(post("/api/hotels")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/hotels/page")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3));
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
}
