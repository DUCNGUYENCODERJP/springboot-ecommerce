package com.luxuryhotel.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luxuryhotel.domain.user.Role;
import com.luxuryhotel.domain.user.User;
import com.luxuryhotel.dto.auth.LoginRequest;
import com.luxuryhotel.dto.user.UpdateUserRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long adminId;

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
        adminId = userRepository.save(admin).getId();
    }

    @Test
    void adminCannotRemoveOwnLastAdminRole() throws Exception {
        String token = login("admin@test.com", "Admin@12345");
        UpdateUserRequest request = new UpdateUserRequest(
                "Test Admin",
                "0900000000",
                true,
                Set.of(Role.CUSTOMER)
        );

        mockMvc.perform(put("/api/users/{userId}", adminId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot remove the last admin role"));
    }

    @Test
    void adminCannotDisableLastAdminAccount() throws Exception {
        String token = login("admin@test.com", "Admin@12345");
        UpdateUserRequest request = new UpdateUserRequest(
                "Test Admin",
                "0900000000",
                false,
                Set.of(Role.ADMIN)
        );

        mockMvc.perform(put("/api/users/{userId}", adminId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot disable the last admin account"));
    }

    @Test
    void adminCanGetPaginatedUsers() throws Exception {
        User customer = new User();
        customer.setFullName("Customer One");
        customer.setEmail("customer1@test.com");
        customer.setPhone("0933333333");
        customer.setPassword(passwordEncoder.encode("Password@123"));
        customer.setEnabled(true);
        customer.setRoles(Set.of(Role.CUSTOMER));
        userRepository.save(customer);

        String token = login("admin@test.com", "Admin@12345");

        mockMvc.perform(get("/api/users/page")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "1")
                        .param("sort", "email,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2));
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
