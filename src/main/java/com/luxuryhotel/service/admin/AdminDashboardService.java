package com.luxuryhotel.service.admin;

import com.luxuryhotel.domain.booking.Booking;
import com.luxuryhotel.domain.booking.BookingStatus;
import com.luxuryhotel.domain.hotel.Hotel;
import com.luxuryhotel.domain.room.Room;
import com.luxuryhotel.domain.user.User;
import com.luxuryhotel.dto.booking.BookingResponse;
import com.luxuryhotel.dto.dashboard.AdminDashboardResponse;
import com.luxuryhotel.dto.dashboard.DashboardSummaryResponse;
import com.luxuryhotel.dto.dashboard.HotelBookingShareResponse;
import com.luxuryhotel.dto.dashboard.HotelRevenueStatResponse;
import com.luxuryhotel.dto.dashboard.RevenueChartResponse;
import com.luxuryhotel.dto.hotel.HotelResponse;
import com.luxuryhotel.mapper.BookingMapper;
import com.luxuryhotel.mapper.HotelMapper;
import com.luxuryhotel.repository.BookingRepository;
import com.luxuryhotel.repository.HotelRepository;
import com.luxuryhotel.repository.RoomRepository;
import com.luxuryhotel.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final BookingRepository bookingRepository;
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final BookingMapper bookingMapper;
    private final HotelMapper hotelMapper;

    public AdminDashboardResponse getDashboard() {
        ZoneId zoneId = ZoneId.systemDefault();
        YearMonth currentMonth = YearMonth.now(zoneId);
        YearMonth previousMonth = currentMonth.minusMonths(1);

        List<Booking> bookings = bookingRepository.findAllWithDetailsOrderByCreatedAtDesc();
        List<Hotel> hotels = hotelRepository.findAll();
        List<Room> rooms = roomRepository.findAll();
        List<User> users = userRepository.findAll();

        Map<Long, HotelStats> hotelStatsMap = new LinkedHashMap<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal revenueThisMonth = BigDecimal.ZERO;
        BigDecimal previousRevenue = BigDecimal.ZERO;
        long bookingsThisMonth = 0;
        long previousBookings = 0;

        List<BigDecimal> currentYearRevenue = zeroedMonthValues();
        List<BigDecimal> previousYearRevenue = zeroedMonthValues();

        for (Booking booking : bookings) {
            if (booking.getCreatedAt() == null) {
                continue;
            }

            ZonedDateTime createdAt = booking.getCreatedAt().atZone(zoneId);
            YearMonth bookingMonth = YearMonth.from(createdAt);
            Hotel hotel = booking.getRoom().getHotel();

            HotelStats stats = hotelStatsMap.computeIfAbsent(hotel.getId(), id -> new HotelStats(hotel));
            stats.incrementBookingCount();

            if (booking.getStatus() != BookingStatus.CANCELLED) {
                totalRevenue = totalRevenue.add(booking.getTotalPrice());
                stats.addRevenue(booking.getTotalPrice());

                int monthIndex = createdAt.getMonthValue() - 1;
                if (createdAt.getYear() == currentMonth.getYear()) {
                    currentYearRevenue.set(monthIndex, currentYearRevenue.get(monthIndex).add(booking.getTotalPrice()));
                    stats.addMonthlyRevenue(monthIndex, booking.getTotalPrice());
                }
                if (createdAt.getYear() == previousMonth.getYear()) {
                    previousYearRevenue.set(monthIndex, previousYearRevenue.get(monthIndex).add(booking.getTotalPrice()));
                    stats.addPreviousMonthRevenue(monthIndex, booking.getTotalPrice());
                }

                if (bookingMonth.equals(currentMonth)) {
                    revenueThisMonth = revenueThisMonth.add(booking.getTotalPrice());
                    stats.incrementBookingsThisMonth();
                    stats.addRevenueThisMonth(booking.getTotalPrice());
                } else if (bookingMonth.equals(previousMonth)) {
                    previousRevenue = previousRevenue.add(booking.getTotalPrice());
                    stats.addRevenuePreviousMonth(booking.getTotalPrice());
                }
            }

            if (bookingMonth.equals(currentMonth)) {
                bookingsThisMonth += 1;
            } else if (bookingMonth.equals(previousMonth)) {
                previousBookings += 1;
            }
        }

        long hotelsThisMonth = hotels.stream()
                .filter(hotel -> hotel.getCreatedAt() != null && YearMonth.from(hotel.getCreatedAt().atZone(zoneId)).equals(currentMonth))
                .count();
        long previousHotels = hotels.stream()
                .filter(hotel -> hotel.getCreatedAt() != null && YearMonth.from(hotel.getCreatedAt().atZone(zoneId)).equals(previousMonth))
                .count();

        long roomsThisMonth = rooms.stream()
                .filter(room -> room.getCreatedAt() != null && YearMonth.from(room.getCreatedAt().atZone(zoneId)).equals(currentMonth))
                .count();
        long previousRooms = rooms.stream()
                .filter(room -> room.getCreatedAt() != null && YearMonth.from(room.getCreatedAt().atZone(zoneId)).equals(previousMonth))
                .count();

        long usersThisMonth = users.stream()
                .filter(user -> user.getCreatedAt() != null && YearMonth.from(user.getCreatedAt().atZone(zoneId)).equals(currentMonth))
                .count();
        long previousUsers = users.stream()
                .filter(user -> user.getCreatedAt() != null && YearMonth.from(user.getCreatedAt().atZone(zoneId)).equals(previousMonth))
                .count();

        List<HotelStats> sortedHotelStats = hotelStatsMap.values().stream()
                .sorted(Comparator.comparingLong(HotelStats::getBookingCount).reversed())
                .toList();

        List<HotelBookingShareResponse> hotelShare = sortedHotelStats.stream()
                .limit(4)
                .map(stats -> new HotelBookingShareResponse(
                        stats.getHotel().getId(),
                        stats.getHotel().getName(),
                        stats.getHotel().getCity(),
                        stats.getBookingCount()
                ))
                .toList();

        List<HotelResponse> featuredHotels = sortedHotelStats.stream()
                .limit(3)
                .map(stats -> hotelMapper.toResponse(stats.getHotel()))
                .toList();

        if (featuredHotels.isEmpty()) {
            featuredHotels = hotels.stream()
                    .sorted(Comparator.comparing(Hotel::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                    .limit(3)
                    .map(hotelMapper::toResponse)
                    .toList();
        }

        List<BookingResponse> recentBookings = bookings.stream()
                .limit(5)
                .map(bookingMapper::toResponse)
                .toList();

        DashboardSummaryResponse summary = new DashboardSummaryResponse(
                bookings.size(),
                bookingsThisMonth,
                hotels.size(),
                hotelsThisMonth,
                rooms.size(),
                roomsThisMonth,
                users.size(),
                usersThisMonth,
                totalRevenue.setScale(2, RoundingMode.HALF_UP),
                revenueThisMonth.setScale(2, RoundingMode.HALF_UP),
                growthPercent(bookingsThisMonth, previousBookings),
                growthPercent(hotelsThisMonth, previousHotels),
                growthPercent(roomsThisMonth, previousRooms),
                growthPercent(usersThisMonth, previousUsers),
                growthPercent(revenueThisMonth, previousRevenue)
        );

        List<HotelRevenueStatResponse> hotelRevenueStats = hotelStatsMap.values().stream()
                .sorted(Comparator.comparing(HotelStats::getRevenue, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(stats -> new HotelRevenueStatResponse(
                        stats.getHotel().getId(),
                        stats.getHotel().getName(),
                        stats.getHotel().getCity(),
                        stats.getHotel().getStarRating(),
                        stats.getRevenue().setScale(2, RoundingMode.HALF_UP),
                        stats.getRevenueThisMonth().setScale(2, RoundingMode.HALF_UP),
                        stats.getBookingCount(),
                        stats.getBookingsThisMonth(),
                        growthPercent(stats.getRevenueThisMonth(), stats.getRevenuePreviousMonth()),
                        stats.getMonthlyRevenue()
                ))
                .toList();

        return new AdminDashboardResponse(
                summary,
                new RevenueChartResponse(monthLabels(), currentYearRevenue, previousYearRevenue),
                hotelShare,
                recentBookings,
                featuredHotels,
                hotelRevenueStats
        );
    }

    private List<BigDecimal> zeroedMonthValues() {
        List<BigDecimal> values = new ArrayList<>(12);
        for (int i = 0; i < 12; i++) {
            values.add(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }
        return values;
    }

    private List<String> monthLabels() {
        return List.of("T1", "T2", "T3", "T4", "T5", "T6", "T7", "T8", "T9", "T10", "T11", "T12");
    }

    private double growthPercent(long current, long previous) {
        if (previous == 0) {
            return current == 0 ? 0.0 : 100.0;
        }
        return roundToTwoDecimals(((double) current - previous) / previous * 100.0);
    }

    private double growthPercent(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) == 0 ? 0.0 : 100.0;
        }
        double currentValue = current.doubleValue();
        double previousValue = previous.doubleValue();
        return roundToTwoDecimals((currentValue - previousValue) / previousValue * 100.0);
    }

    private double roundToTwoDecimals(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static final class HotelStats {
        private final Hotel hotel;
        private long bookingCount;
        private long bookingsThisMonth;
        private BigDecimal revenue;
        private BigDecimal revenueThisMonth;
        private BigDecimal revenuePreviousMonth;
        private final List<BigDecimal> monthlyRevenue;
        private final List<BigDecimal> previousMonthlyRevenue;

        private HotelStats(Hotel hotel) {
            this.hotel = hotel;
            this.bookingCount = 0L;
            this.bookingsThisMonth = 0L;
            this.revenue = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            this.revenueThisMonth = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            this.revenuePreviousMonth = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            this.monthlyRevenue = new ArrayList<>();
            this.previousMonthlyRevenue = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
                this.monthlyRevenue.add(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                this.previousMonthlyRevenue.add(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            }
        }

        private Hotel getHotel() { return hotel; }
        private long getBookingCount() { return bookingCount; }
        private long getBookingsThisMonth() { return bookingsThisMonth; }
        private BigDecimal getRevenue() { return revenue; }
        private BigDecimal getRevenueThisMonth() { return revenueThisMonth; }
        private BigDecimal getRevenuePreviousMonth() { return revenuePreviousMonth; }
        private List<BigDecimal> getMonthlyRevenue() { return monthlyRevenue; }

        private void incrementBookingCount() { bookingCount += 1; }
        private void incrementBookingsThisMonth() { bookingsThisMonth += 1; }

        private void addRevenue(BigDecimal amount) {
            revenue = revenue.add(amount);
        }

        private void addRevenueThisMonth(BigDecimal amount) {
            revenueThisMonth = revenueThisMonth.add(amount);
        }

        private void addRevenuePreviousMonth(BigDecimal amount) {
            revenuePreviousMonth = revenuePreviousMonth.add(amount);
        }

        private void addMonthlyRevenue(int monthIndex, BigDecimal amount) {
            monthlyRevenue.set(monthIndex, monthlyRevenue.get(monthIndex).add(amount));
        }

        private void addPreviousMonthRevenue(int monthIndex, BigDecimal amount) {
            previousMonthlyRevenue.set(monthIndex, previousMonthlyRevenue.get(monthIndex).add(amount));
        }
    }
}
