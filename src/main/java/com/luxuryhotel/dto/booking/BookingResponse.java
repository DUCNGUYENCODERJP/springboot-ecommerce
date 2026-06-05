package com.luxuryhotel.dto.booking;

import com.luxuryhotel.domain.booking.BookingStatus;
import com.luxuryhotel.dto.room.RoomResponse;
import com.luxuryhotel.dto.user.UserResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record BookingResponse(
        Long id,
        String bookingCode,
        UserResponse user,
        RoomResponse room,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        Integer guestCount,
        BigDecimal totalPrice,
        BookingStatus status,
        String specialRequest,
        Instant createdAt,
        Instant updatedAt
) {
}
