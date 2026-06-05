package com.luxuryhotel.mapper;

import com.luxuryhotel.domain.hotel.Hotel;
import com.luxuryhotel.domain.room.Room;
import com.luxuryhotel.dto.hotel.HotelSummaryResponse;
import com.luxuryhotel.dto.room.RoomResponse;
import org.springframework.stereotype.Component;
import lombok.Data;

@Component
@Data
public class RoomMapper {

    private final HotelMapper hotelMapper;

    public RoomMapper(HotelMapper hotelMapper) {
        this.hotelMapper = hotelMapper;
    }

    public RoomResponse toResponse(Room room) {
        Hotel hotel = room.getHotel();
        return new RoomResponse(
                room.getId(),
                hotelMapper.toSummary(hotel),
                room.getRoomNumber(),
                room.getType(),
                room.getStatus(),
                room.getPricePerNight(),
                room.getCapacity(),
                room.getFloor(),
                room.getDescription(),
                room.getHasWifi(),
                room.getHasBreakfast(),
                room.getCreatedAt(),
                room.getUpdatedAt()
        );
    }
}
