package com.luxuryhotel.controller;

import com.luxuryhotel.dto.common.PageResponse;
import com.luxuryhotel.dto.booking.BookingResponse;
import com.luxuryhotel.dto.booking.CreateBookingRequest;
import com.luxuryhotel.service.booking.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse createBooking(@Valid @RequestBody CreateBookingRequest request) {
        return bookingService.createBooking(request);
    }

    @GetMapping("/my")
    public List<BookingResponse> getMyBookings() {
        return bookingService.getMyBookings();
    }

    @GetMapping("/my/page")
    public PageResponse<BookingResponse> getMyBookingsPage(Pageable pageable) {
        return bookingService.getMyBookingsPage(pageable);
    }

    @GetMapping("/{bookingId}")
    public BookingResponse getBookingById(@PathVariable Long bookingId) {
        return bookingService.getBookingById(bookingId);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<BookingResponse> getAllBookings(@RequestParam(required = false) Long hotelId) {
        return bookingService.getAllBookings(hotelId);
    }

    @GetMapping("/page")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<BookingResponse> getBookingsPage(@RequestParam(required = false) Long hotelId,
                                                         Pageable pageable) {
        return bookingService.getBookingsPage(hotelId, pageable);
    }

    @PatchMapping("/{bookingId}/cancel")
    public BookingResponse cancelMyBooking(@PathVariable Long bookingId) {
        return bookingService.cancelMyBooking(bookingId);
    }

    @PatchMapping("/{bookingId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public BookingResponse updateBookingStatus(@PathVariable Long bookingId, @RequestParam com.luxuryhotel.domain.booking.BookingStatus status) {
        return bookingService.updateBookingStatus(bookingId, status);
    }
}
