package com.luxuryhotel.dto.auth;

import com.luxuryhotel.dto.user.UserResponse;

public record AuthResponse(
        String accessToken,
        String tokenType,
        UserResponse user
) {
}
