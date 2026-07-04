package com.luxuryhotel.controller;

import com.luxuryhotel.domain.booking.Booking;
import com.luxuryhotel.domain.booking.BookingStatus;
import com.luxuryhotel.domain.hotel.Hotel;
import com.luxuryhotel.domain.room.Room;
import com.luxuryhotel.domain.room.RoomStatus;
import com.luxuryhotel.domain.room.RoomType;
import com.luxuryhotel.domain.user.Role;
import com.luxuryhotel.domain.user.User;
import com.luxuryhotel.repository.BookingRepository;
import com.luxuryhotel.repository.HotelRepository;
import com.luxuryhotel.repository.RoomRepository;
import com.luxuryhotel.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/test-data")
@RequiredArgsConstructor
public class TestDataController {

    private final UserRepository userRepository;
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/seed")
    public String seedData() {
        // 1. Create a test user
        String email = "testuser@example.com";
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            user = new User();
            user.setFullName("Nguyễn Văn Test");
            user.setEmail(email);
            user.setPhone("09" + (int)(Math.random() * 100000000));
            user.setPassword(passwordEncoder.encode("123456"));
            user.setRoles(Set.of(Role.CUSTOMER));
            user.setEnabled(true);
            user = userRepository.save(user);
        }

        // 2. Create a test hotel
        Hotel hotel = hotelRepository.findAll().stream().findFirst().orElse(null);
        if (hotel == null) {
            hotel = new Hotel();
            hotel.setName("Khách Sạn Luxury Test");
            hotel.setAddress("123 Đường Test");
            hotel.setCity("Hà Nội");
            hotel.setCountry("Việt Nam");
            hotel.setStarRating(5);
            hotel.setDescription("Khách sạn tuyệt vời để thử nghiệm đánh giá.");
            hotel = hotelRepository.save(hotel);
        }

        // 3. Create a test room
        Room room = roomRepository.findByHotelId(hotel.getId()).stream().findFirst().orElse(null);
        if (room == null) {
            room = new Room();
            room.setHotel(hotel);
            room.setRoomNumber("T101");
            room.setType(RoomType.DELUXE);
            room.setStatus(RoomStatus.AVAILABLE);
            room.setPricePerNight(BigDecimal.valueOf(1000000));
            room.setCapacity(2);
            room.setFloor(1);
            room.setDescription("Phòng xịn xò.");
            room.setHasWifi(true);
            room.setHasBreakfast(true);
            room = roomRepository.save(room);
        }

        // 4. Create a CHECKED_OUT booking for the test user
        boolean hasBooking = bookingRepository.findByUserId(user.getId()).stream()
                .anyMatch(b -> b.getStatus() == BookingStatus.CHECKED_OUT);

        if (!hasBooking) {
            Booking booking = new Booking();
            booking.setBookingCode("TEST-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
            booking.setUser(user);
            booking.setRoom(room);
            booking.setCheckInDate(LocalDate.now().minusDays(3));
            booking.setCheckOutDate(LocalDate.now().minusDays(1));
            booking.setGuestCount(2);
            booking.setTotalPrice(BigDecimal.valueOf(2000000));
            booking.setStatus(BookingStatus.CHECKED_OUT);
            booking.setSpecialRequest("Phòng view biển");
            bookingRepository.save(booking);
        }

        return "Data seeded successfully! You can login with: " + email + " / 123456";
    }
}
