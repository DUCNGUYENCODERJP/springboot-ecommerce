package com.luxuryhotel.dto.dashboard;

public record HotelBookingShareResponse(
        Long hotelId,
        String hotelName,
        String city,
        long bookingCount
) {
}
