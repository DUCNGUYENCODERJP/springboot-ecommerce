package com.luxuryhotel.config;

import com.luxuryhotel.config.AdminProperties;
import com.luxuryhotel.domain.user.Role;
import com.luxuryhotel.domain.user.User;
import com.luxuryhotel.repository.UserRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Component
@EnableConfigurationProperties(AdminProperties.class)
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder, AdminProperties adminProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminProperties = adminProperties;
    }

    @Override
    public void run(String... args) {
        String adminEmail = adminProperties.email().trim().toLowerCase();
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }

        User admin = User.builder()
                .fullName(adminProperties.fullName().trim())
                .email(adminEmail)
                .phone(adminProperties.phone())
                .password(passwordEncoder.encode(adminProperties.password()))
                .enabled(true)
                .roles(Set.of(Role.ADMIN))
                .build();

        userRepository.save(admin);
    }
}
