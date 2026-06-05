package com.luxuryhotel.service.room;

import com.luxuryhotel.domain.hotel.Hotel;
import com.luxuryhotel.domain.room.Room;
import com.luxuryhotel.dto.common.PageResponse;
import com.luxuryhotel.dto.room.RoomRequest;
import com.luxuryhotel.dto.room.RoomResponse;
import com.luxuryhotel.mapper.RoomMapper;
import com.luxuryhotel.repository.BookingRepository;
import com.luxuryhotel.repository.HotelRepository;
import com.luxuryhotel.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.luxuryhotel.domain.booking.BookingStatus.CHECKED_IN;
import static com.luxuryhotel.domain.booking.BookingStatus.CONFIRMED;
import static com.luxuryhotel.domain.booking.BookingStatus.PENDING;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final BookingRepository bookingRepository;
    private final RoomMapper roomMapper;

    @Transactional(readOnly = true)
    public List<RoomResponse> getAllRooms(Long hotelId) {
        List<Room> rooms = hotelId == null ? roomRepository.findAll() : roomRepository.findByHotelId(hotelId);
        return rooms.stream().map(roomMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<RoomResponse> getRoomsPage(Long hotelId, Pageable pageable) {
        Page<Room> rooms = hotelId == null
                ? roomRepository.findAll(pageable)
                : roomRepository.findByHotelId(hotelId, pageable);
        return PageResponse.from(rooms.map(roomMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public RoomResponse getRoomById(Long roomId) {
        return roomMapper.toResponse(findRoomById(roomId));
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> searchAvailableRooms(Long hotelId,
                                                   LocalDate checkInDate,
                                                   LocalDate checkOutDate,
                                                   Integer guestCount,
                                                   BigDecimal maxPrice) {
        validateDateRange(checkInDate, checkOutDate);

        int minimumCapacity = guestCount == null ? 1 : guestCount;
        BigDecimal maximumPrice = maxPrice == null ? new BigDecimal("999999999") : maxPrice;

        return roomRepository.searchAvailableRooms(
                        hotelId,
                        checkInDate,
                        checkOutDate,
                        minimumCapacity,
                        maximumPrice,
                        List.of(PENDING, CONFIRMED, CHECKED_IN)
                ).stream()
                .map(roomMapper::toResponse)
                .toList();
    }

    @Transactional
    public RoomResponse createRoom(RoomRequest request) {
        if (roomRepository.existsByRoomNumber(request.roomNumber().trim())) {
            throw new IllegalArgumentException("Room number already exists");
        }

        Hotel hotel = findHotelById(request.hotelId());
        Room room = new Room();
        applyRoomRequest(room, request, hotel);
        return roomMapper.toResponse(roomRepository.save(room));
    }

    @Transactional
    public RoomResponse updateRoom(Long roomId, RoomRequest request) {
        Room room = findRoomById(roomId);
        String newRoomNumber = request.roomNumber().trim();
        if (!room.getRoomNumber().equals(newRoomNumber) && roomRepository.existsByRoomNumber(newRoomNumber)) {
            throw new IllegalArgumentException("Room number already exists");
        }

        Hotel hotel = findHotelById(request.hotelId());
        applyRoomRequest(room, request, hotel);
        return roomMapper.toResponse(roomRepository.save(room));
    }

    @Transactional
    public void deleteRoom(Long roomId) {
        Room room = findRoomById(roomId);
        if (bookingRepository.existsByRoomId(roomId)) {
            throw new IllegalStateException("Cannot delete room with existing bookings");
        }
        roomRepository.delete(room);
    }

    private void applyRoomRequest(Room room, RoomRequest request, Hotel hotel) {
        room.setHotel(hotel);
        room.setRoomNumber(request.roomNumber().trim());
        room.setType(request.type());
        room.setStatus(request.status());
        room.setPricePerNight(request.pricePerNight());
        room.setCapacity(request.capacity());
        room.setFloor(request.floor());
        room.setDescription(request.description() == null ? null : request.description().trim());
        room.setHasWifi(request.hasWifi());
        room.setHasBreakfast(request.hasBreakfast());
    }

    private Room findRoomById(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));
    }

    private Hotel findHotelById(Long hotelId) {
        return hotelRepository.findById(hotelId)
                .orElseThrow(() -> new IllegalArgumentException("Hotel not found"));
    }

    private void validateDateRange(LocalDate checkInDate, LocalDate checkOutDate) {
        if (checkInDate == null || checkOutDate == null) {
            throw new IllegalArgumentException("Check-in date and check-out date are required");
        }
        if (!checkOutDate.isAfter(checkInDate)) {
            throw new IllegalArgumentException("Check-out date must be after check-in date");
        }
    }
}
