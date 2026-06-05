package com.luxuryhotel.dto.booking;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateBookingRequest(
        @NotNull(message = "Room id is required")
        Long roomId,

        @NotNull(message = "Check-in date is required")
        @FutureOrPresent(message = "Check-in date must be today or later")
        LocalDate checkInDate,

        @NotNull(message = "Check-out date is required")
        LocalDate checkOutDate,

        @NotNull(message = "Guest count is required")
        @Min(value = 1, message = "Guest count must be at least 1")
        Integer guestCount,

        @Size(max = 500, message = "Special request must be at most 500 characters")
        String specialRequest
) {
}
