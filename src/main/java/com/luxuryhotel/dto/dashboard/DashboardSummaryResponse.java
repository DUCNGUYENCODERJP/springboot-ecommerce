package com.luxuryhotel.dto.dashboard;

import java.math.BigDecimal;

public record DashboardSummaryResponse(
        long totalBookings,
        long bookingsThisMonth,
        long totalHotels,
        long hotelsThisMonth,
        long totalRooms,
        long roomsThisMonth,
        long totalUsers,
        long usersThisMonth,
        BigDecimal totalRevenue,
        BigDecimal revenueThisMonth,
        double bookingGrowthPercent,
        double hotelGrowthPercent,
        double roomGrowthPercent,
        double userGrowthPercent,
        double revenueGrowthPercent
) {
}
