package com.luxuryhotel.dto.room;

import com.luxuryhotel.domain.room.RoomStatus;
import com.luxuryhotel.domain.room.RoomType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RoomRequest(
        @NotNull(message = "Hotel id is required")
        Long hotelId,

        @NotBlank(message = "Room number is required")
        @Size(max = 30, message = "Room number must be at most 30 characters")
        String roomNumber,

        @NotNull(message = "Room type is required")
        RoomType type,

        @NotNull(message = "Room status is required")
        RoomStatus status,

        @NotNull(message = "Price per night is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Price per night must be greater than 0")
        BigDecimal pricePerNight,

        @NotNull(message = "Capacity is required")
        @Min(value = 1, message = "Capacity must be at least 1")
        Integer capacity,

        @NotNull(message = "Floor is required")
        Integer floor,

        @Size(max = 1000, message = "Description must be at most 1000 characters")
        String description,

        @NotNull(message = "Wifi flag is required")
        Boolean hasWifi,

        @NotNull(message = "Breakfast flag is required")
        Boolean hasBreakfast
) {
}
