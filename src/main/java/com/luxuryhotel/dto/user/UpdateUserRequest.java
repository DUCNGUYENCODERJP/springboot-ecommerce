package com.luxuryhotel.dto.user;

import com.luxuryhotel.domain.user.Role;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UpdateUserRequest(
        @Size(max = 100, message = "Full name must be at most 100 characters")
        String fullName,

        @Size(max = 20, message = "Phone must be at most 20 characters")
        String phone,

        @NotNull(message = "Enabled flag is required")
        Boolean enabled,

        @NotEmpty(message = "At least one role is required")
        Set<Role> roles
) {
}
