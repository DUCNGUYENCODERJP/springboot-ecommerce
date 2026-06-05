package com.luxuryhotel.dto.hotel;

public record HotelSummaryResponse(
        Long id,
        String name,
        String city,
        String country
) {
}
