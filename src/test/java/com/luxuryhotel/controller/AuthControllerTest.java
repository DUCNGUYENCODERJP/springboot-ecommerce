package com.luxuryhotel.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luxuryhotel.domain.user.Role;
import com.luxuryhotel.domain.user.User;
import com.luxuryhotel.dto.auth.LoginRequest;
import com.luxuryhotel.dto.auth.RegisterRequest;
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

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
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
    void registerShouldCreateCustomerAndReturnToken() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Nguyen Van A",
                "customer@example.com",
                "0912345678",
                "Password@123"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("customer@example.com"))
                .andExpect(jsonPath("$.user.roles[0]").value("CUSTOMER"));
    }

    @Test
    void loginShouldReturnTokenForValidCredentials() throws Exception {
        User user = new User();
        user.setFullName("Existing User");
        user.setEmail("existing@example.com");
        user.setPhone("0987654321");
        user.setPassword(passwordEncoder.encode("Password@123"));
        user.setEnabled(true);
        user.setRoles(Set.of(Role.CUSTOMER));
        userRepository.save(user);

        LoginRequest request = new LoginRequest("existing@example.com", "Password@123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("existing@example.com"));
    }

    @Test
    void meShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication is required to access this resource"));
    }
}
