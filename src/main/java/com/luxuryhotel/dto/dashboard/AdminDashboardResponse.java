package com.luxuryhotel.dto.dashboard;

import com.luxuryhotel.dto.booking.BookingResponse;
import com.luxuryhotel.dto.hotel.HotelResponse;

import java.util.List;

public record AdminDashboardResponse(
        DashboardSummaryResponse summary,
        RevenueChartResponse revenueChart,
        List<HotelBookingShareResponse> hotelShare,
        List<BookingResponse> recentBookings,
        List<HotelResponse> featuredHotels,
        List<HotelRevenueStatResponse> hotelRevenueStats
) {
}
