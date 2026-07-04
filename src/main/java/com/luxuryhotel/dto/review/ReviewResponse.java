package com.luxuryhotel.dto.review;

import java.time.Instant;

public record ReviewResponse(
        Long id,
        Long bookingId,
        String bookingCode,
        Long roomId,
        String roomNumber,
        Long userId,
        String userFullName,
        Integer rating,
        String comment,
        Instant createdAt,
        Instant updatedAt
) {
}
