package com.luxuryhotel.dto.hotel;

import java.time.Instant;

public record HotelResponse(
        Long id,
        String name,
        String address,
        String city,
        String country,
        Integer starRating,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}
