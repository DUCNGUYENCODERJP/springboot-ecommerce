package com.luxuryhotel.service.booking;

import com.luxuryhotel.domain.booking.Booking;
import com.luxuryhotel.domain.booking.BookingStatus;
import com.luxuryhotel.domain.room.Room;
import com.luxuryhotel.domain.room.RoomStatus;
import com.luxuryhotel.domain.user.User;
import com.luxuryhotel.dto.booking.BookingResponse;
import com.luxuryhotel.dto.booking.CreateBookingRequest;
import com.luxuryhotel.mapper.BookingMapper;
import com.luxuryhotel.repository.BookingRepository;
import com.luxuryhotel.repository.RoomRepository;
import com.luxuryhotel.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserService userService;

    @Mock
    private BookingMapper bookingMapper;

    @InjectMocks
    private BookingService bookingService;

    private User testUser;
    private Room testRoom;
    private CreateBookingRequest testRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@test.com");

        testRoom = new Room();
        testRoom.setId(10L);
        testRoom.setStatus(RoomStatus.AVAILABLE);
        testRoom.setCapacity(2);
        testRoom.setPricePerNight(BigDecimal.valueOf(1000000));

        testRequest = new CreateBookingRequest(
                10L,
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(3),
                2,
                "Need extra towels"
        );
    }

    @Test
    void createBooking_Success() {
        // Arrange
        when(userService.getCurrentUserEntity()).thenReturn(testUser);
        when(roomRepository.findByIdForUpdate(testRequest.roomId())).thenReturn(Optional.of(testRoom));
        when(bookingRepository.existsByRoomIdAndStatusInAndCheckInDateLessThanAndCheckOutDateGreaterThan(
                eq(testRoom.getId()), anyList(), eq(testRequest.checkOutDate()), eq(testRequest.checkInDate())
        )).thenReturn(false);

        Booking savedBooking = new Booking();
        savedBooking.setId(100L);
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

        BookingResponse mockResponse = new BookingResponse(
                100L, "BK-TEST", null, null, testRequest.checkInDate(), testRequest.checkOutDate(),
                2, BigDecimal.valueOf(2000000), BookingStatus.CONFIRMED, "Need extra towels", null, null
        );
        when(bookingMapper.toResponse(any(Booking.class))).thenReturn(mockResponse);

        // Act
        BookingResponse response = bookingService.createBooking(testRequest);

        // Assert
        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals("BK-TEST", response.bookingCode());
        assertEquals(BigDecimal.valueOf(2000000), response.totalPrice());
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void createBooking_Fails_WhenRoomNotAvailable() {
        // Arrange
        testRoom.setStatus(RoomStatus.MAINTENANCE);
        when(userService.getCurrentUserEntity()).thenReturn(testUser);
        when(roomRepository.findByIdForUpdate(testRequest.roomId())).thenReturn(Optional.of(testRoom));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            bookingService.createBooking(testRequest);
        });

        assertEquals("Room is not available for booking", exception.getMessage());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void createBooking_Fails_WhenGuestCountExceedsCapacity() {
        // Arrange
        CreateBookingRequest requestExceedsCapacity = new CreateBookingRequest(
                10L, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3),
                3, null
        );
        when(userService.getCurrentUserEntity()).thenReturn(testUser);
        when(roomRepository.findByIdForUpdate(requestExceedsCapacity.roomId())).thenReturn(Optional.of(testRoom));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            bookingService.createBooking(requestExceedsCapacity);
        });

        assertEquals("Guest count exceeds room capacity", exception.getMessage());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void createBooking_Fails_WhenRoomAlreadyBooked() {
        // Arrange
        when(userService.getCurrentUserEntity()).thenReturn(testUser);
        when(roomRepository.findByIdForUpdate(testRequest.roomId())).thenReturn(Optional.of(testRoom));
        when(bookingRepository.existsByRoomIdAndStatusInAndCheckInDateLessThanAndCheckOutDateGreaterThan(
                eq(testRoom.getId()), anyList(), eq(testRequest.checkOutDate()), eq(testRequest.checkInDate())
        )).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            bookingService.createBooking(testRequest);
        });

        assertEquals("Room is already booked for the selected date range", exception.getMessage());
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void createBooking_Fails_WhenDatesInvalid() {
        // Arrange
        CreateBookingRequest invalidRequest = new CreateBookingRequest(
                10L, LocalDate.now().plusDays(3), LocalDate.now().plusDays(1),
                2, null
        );

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            bookingService.createBooking(invalidRequest);
        });

        assertEquals("Check-out date must be after check-in date", exception.getMessage());
        verify(roomRepository, never()).findByIdForUpdate(anyLong());
    }
}
