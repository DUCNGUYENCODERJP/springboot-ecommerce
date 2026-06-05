package com.luxuryhotel.dto.user;

import com.luxuryhotel.domain.user.Role;

import java.time.Instant;
import java.util.Set;

public record UserResponse(
        Long id,
        String fullName,
        String email,
        String phone,
        Boolean enabled,
        Set<Role> roles,
        Instant createdAt,
        Instant updatedAt
) {
}
