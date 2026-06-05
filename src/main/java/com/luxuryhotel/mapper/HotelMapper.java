package com.luxuryhotel.mapper;

import com.luxuryhotel.domain.hotel.Hotel;
import com.luxuryhotel.dto.hotel.HotelResponse;
import com.luxuryhotel.dto.hotel.HotelSummaryResponse;
import org.springframework.stereotype.Component;

@Component
public class HotelMapper {

    public HotelResponse toResponse(Hotel hotel) {
        return new HotelResponse(
                hotel.getId(),
                hotel.getName(),
                hotel.getAddress(),
                hotel.getCity(),
                hotel.getCountry(),
                hotel.getStarRating(),
                hotel.getDescription(),
                hotel.getCreatedAt(),
                hotel.getUpdatedAt()
        );
    }

    public HotelSummaryResponse toSummary(Hotel hotel) {
        return new HotelSummaryResponse(
                hotel.getId(),
                hotel.getName(),
                hotel.getCity(),
                hotel.getCountry()
        );
    }
}
