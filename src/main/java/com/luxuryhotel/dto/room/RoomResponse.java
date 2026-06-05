package com.luxuryhotel.dto.room;

import com.luxuryhotel.domain.room.RoomStatus;
import com.luxuryhotel.domain.room.RoomType;
import com.luxuryhotel.dto.hotel.HotelSummaryResponse;

import java.math.BigDecimal;
import java.time.Instant;

public record RoomResponse(
        Long id,
        HotelSummaryResponse hotel,
        String roomNumber,
        RoomType type,
        RoomStatus status,
        BigDecimal pricePerNight,
        Integer capacity,
        Integer floor,
        String description,
        Boolean hasWifi,
        Boolean hasBreakfast,
        Instant createdAt,
        Instant updatedAt
) {
}
