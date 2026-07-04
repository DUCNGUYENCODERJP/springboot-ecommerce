package com.luxuryhotel.service.booking;

import com.luxuryhotel.domain.booking.Booking;
import com.luxuryhotel.domain.booking.BookingStatus;
import com.luxuryhotel.domain.room.Room;
import com.luxuryhotel.domain.room.RoomStatus;
import com.luxuryhotel.domain.user.User;
import com.luxuryhotel.dto.common.PageResponse;
import com.luxuryhotel.dto.booking.BookingResponse;
import com.luxuryhotel.dto.booking.CreateBookingRequest;
import com.luxuryhotel.mapper.BookingMapper;
import com.luxuryhotel.repository.BookingRepository;
import com.luxuryhotel.repository.RoomRepository;
import com.luxuryhotel.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final UserService userService;
    private final BookingMapper bookingMapper;

    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request) {
        validateBookingDates(request);

        User currentUser = userService.getCurrentUserEntity();
        Room room = roomRepository.findByIdForUpdate(request.roomId())
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        if (room.getStatus() != RoomStatus.AVAILABLE) {
            throw new IllegalArgumentException("Room is not available for booking");
        }
        if (request.guestCount() > room.getCapacity()) {
            throw new IllegalArgumentException("Guest count exceeds room capacity");
        }

        boolean alreadyBooked = bookingRepository.existsByRoomIdAndStatusInAndCheckInDateLessThanAndCheckOutDateGreaterThan(
                room.getId(),
                List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN),
                request.checkOutDate(),
                request.checkInDate()
        );
        if (alreadyBooked) {
            throw new IllegalArgumentException("Room is already booked for the selected date range");
        }

        long nights = ChronoUnit.DAYS.between(request.checkInDate(), request.checkOutDate());
        BigDecimal totalPrice = room.getPricePerNight()
                .multiply(BigDecimal.valueOf(nights))
                .setScale(2, RoundingMode.HALF_UP);

        Booking booking = new Booking();
        booking.setBookingCode(generateBookingCode());
        booking.setUser(currentUser);
        booking.setRoom(room);
        booking.setCheckInDate(request.checkInDate());
        booking.setCheckOutDate(request.checkOutDate());
        booking.setGuestCount(request.guestCount());
        booking.setTotalPrice(totalPrice);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setSpecialRequest(request.specialRequest() == null ? null : request.specialRequest().trim());

        return bookingMapper.toResponse(bookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings() {
        User currentUser = userService.getCurrentUserEntity();
        return bookingRepository.findByUserId(currentUser.getId())
                .stream()
                .map(bookingMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> getMyBookingsPage(Pageable pageable) {
        User currentUser = userService.getCurrentUserEntity();
        return PageResponse.from(bookingRepository.findByUserId(currentUser.getId(), pageable).map(bookingMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long bookingId) {
        User currentUser = userService.getCurrentUserEntity();
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        boolean isAdmin = currentUser.getRoles().stream().anyMatch(role -> role.name().equals("ADMIN"));
        boolean isOwner = booking.getUser().getId().equals(currentUser.getId());
        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("You are not allowed to access this booking");
        }

        return bookingMapper.toResponse(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getAllBookings(Long hotelId) {
        return bookingRepository.findAllByHotelId(hotelId)
                .stream()
                .map(bookingMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> getBookingsPage(Long hotelId, Pageable pageable) {
        return PageResponse.from(bookingRepository.findAllByHotelId(hotelId, pageable).map(bookingMapper::toResponse));
    }

    @Transactional
    public BookingResponse cancelMyBooking(Long bookingId) {
        User currentUser = userService.getCurrentUserEntity();
        Booking booking = bookingRepository.findByIdAndUserId(bookingId, currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalArgumentException("Booking is already cancelled");
        }
        if (booking.getStatus() == BookingStatus.CHECKED_IN) {
            throw new IllegalArgumentException("Cannot cancel a booking that is already checked in");
        }
        if (!booking.getCheckInDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Only future bookings can be cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        return bookingMapper.toResponse(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse updateBookingStatus(Long bookingId, BookingStatus status) {
        User currentUser = userService.getCurrentUserEntity();
        boolean isAdmin = currentUser.getRoles().stream().anyMatch(role -> role.name().equals("ADMIN"));
        if (!isAdmin) {
            throw new AccessDeniedException("Only administrators can update booking status");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
                
        // Optional validation: you could prevent checking out if not checked in, etc.
        // For flexibility, admins can set it to any status.

        booking.setStatus(status);
        return bookingMapper.toResponse(bookingRepository.save(booking));
    }

    private void validateBookingDates(CreateBookingRequest request) {
        if (request.checkOutDate() == null || !request.checkOutDate().isAfter(request.checkInDate())) {
            throw new IllegalArgumentException("Check-out date must be after check-in date");
        }
    }

    private String generateBookingCode() {
        return "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
