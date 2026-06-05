package com.luxuryhotel.service.auth;

import com.luxuryhotel.domain.user.Role;
import com.luxuryhotel.domain.user.User;
import com.luxuryhotel.dto.auth.AuthResponse;
import com.luxuryhotel.dto.auth.LoginRequest;
import com.luxuryhotel.dto.auth.RegisterRequest;
import com.luxuryhotel.mapper.UserMapper;
import com.luxuryhotel.repository.UserRepository;
import com.luxuryhotel.service.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        String normalizedPhone = request.phone().trim();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email is already in use");
        }
        if (userRepository.existsByPhone(normalizedPhone)) {
            throw new IllegalArgumentException("Phone is already in use");
        }

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(normalizedEmail);
        user.setPhone(normalizedPhone);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEnabled(true);
        user.setRoles(Set.of(Role.CUSTOMER));

        User savedUser = userRepository.save(user);
        return buildAuthResponse(savedUser);
    }

    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
        );

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new IllegalStateException("User account is disabled");
        }

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String token = jwtService.generateToken(userDetails, Map.of(
                "userId", user.getId(),
                "roles", user.getRoles()
        ));

        return new AuthResponse(token, "Bearer", userMapper.toResponse(user));
    }
}
