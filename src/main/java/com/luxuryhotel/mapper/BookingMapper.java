package com.luxuryhotel.mapper;

import com.luxuryhotel.domain.booking.Booking;
import com.luxuryhotel.dto.booking.BookingResponse;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {

    private final UserMapper userMapper;
    private final RoomMapper roomMapper;

    public BookingMapper(UserMapper userMapper, RoomMapper roomMapper) {
        this.userMapper = userMapper;
        this.roomMapper = roomMapper;
    }

    public BookingResponse toResponse(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getBookingCode(),
                userMapper.toResponse(booking.getUser()),
                roomMapper.toResponse(booking.getRoom()),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getGuestCount(),
                booking.getTotalPrice(),
                booking.getStatus(),
                booking.getSpecialRequest(),
                booking.getCreatedAt(),
                booking.getUpdatedAt()
        );
    }
}
