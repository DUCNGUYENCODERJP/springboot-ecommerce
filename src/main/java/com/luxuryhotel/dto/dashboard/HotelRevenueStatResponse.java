package com.luxuryhotel.dto.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record HotelRevenueStatResponse(
        Long hotelId,
        String hotelName,
        String city,
        Integer starRating,
        BigDecimal totalRevenue,
        BigDecimal revenueThisMonth,
        long totalBookings,
        long bookingsThisMonth,
        double revenueGrowthPercent,
        List<BigDecimal> monthlyRevenue
) {
}
