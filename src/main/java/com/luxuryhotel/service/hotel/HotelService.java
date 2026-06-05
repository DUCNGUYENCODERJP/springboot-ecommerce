package com.luxuryhotel.service.hotel;

import com.luxuryhotel.domain.hotel.Hotel;
import com.luxuryhotel.dto.common.PageResponse;
import com.luxuryhotel.dto.hotel.HotelRequest;
import com.luxuryhotel.dto.hotel.HotelResponse;
import com.luxuryhotel.mapper.HotelMapper;
import com.luxuryhotel.repository.HotelRepository;
import com.luxuryhotel.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HotelService {

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final HotelMapper hotelMapper;

    @Transactional(readOnly = true)
    public List<HotelResponse> getAllHotels() {
        return hotelRepository.findAll()
                .stream()
                .map(hotelMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<HotelResponse> getHotelsPage(Pageable pageable) {
        return PageResponse.from(hotelRepository.findAll(pageable).map(hotelMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public HotelResponse getHotelById(Long hotelId) {
        return hotelMapper.toResponse(findHotelById(hotelId));
    }

    @Transactional
    public HotelResponse createHotel(HotelRequest request) {
        Hotel hotel = new Hotel();
        applyRequest(hotel, request);
        return hotelMapper.toResponse(hotelRepository.save(hotel));
    }

    @Transactional
    public HotelResponse updateHotel(Long hotelId, HotelRequest request) {
        Hotel hotel = findHotelById(hotelId);
        applyRequest(hotel, request);
        return hotelMapper.toResponse(hotelRepository.save(hotel));
    }

    @Transactional
    public void deleteHotel(Long hotelId) {
        Hotel hotel = findHotelById(hotelId);
        if (roomRepository.existsByHotelId(hotelId)) {
            throw new IllegalStateException("Cannot delete hotel while rooms still exist");
        }
        hotelRepository.delete(hotel);
    }

    private Hotel findHotelById(Long hotelId) {
        return hotelRepository.findById(hotelId)
                .orElseThrow(() -> new IllegalArgumentException("Hotel not found"));
    }

    private void applyRequest(Hotel hotel, HotelRequest request) {
        hotel.setName(request.name().trim());
        hotel.setAddress(request.address().trim());
        hotel.setCity(request.city().trim());
        hotel.setCountry(request.country().trim());
        hotel.setStarRating(request.starRating());
        hotel.setDescription(request.description() == null ? null : request.description().trim());
    }
}
