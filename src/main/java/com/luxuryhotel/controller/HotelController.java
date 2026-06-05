package com.luxuryhotel.controller;

import com.luxuryhotel.dto.common.PageResponse;
import com.luxuryhotel.dto.hotel.HotelRequest;
import com.luxuryhotel.dto.hotel.HotelResponse;
import com.luxuryhotel.service.hotel.HotelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/hotels")
@RequiredArgsConstructor
public class HotelController {

    private final HotelService hotelService;

    @GetMapping
    public List<HotelResponse> getAllHotels() {
        return hotelService.getAllHotels();
    }

    @GetMapping("/page")
    public PageResponse<HotelResponse> getHotelsPage(Pageable pageable) {
        return hotelService.getHotelsPage(pageable);
    }

    @GetMapping("/{hotelId}")
    public HotelResponse getHotelById(@PathVariable Long hotelId) {
        return hotelService.getHotelById(hotelId);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public HotelResponse createHotel(@Valid @RequestBody HotelRequest request) {
        return hotelService.createHotel(request);
    }

    @PutMapping("/{hotelId}")
    @PreAuthorize("hasRole('ADMIN')")
    public HotelResponse updateHotel(@PathVariable Long hotelId,
                                     @Valid @RequestBody HotelRequest request) {
        return hotelService.updateHotel(hotelId, request);
    }

    @DeleteMapping("/{hotelId}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHotel(@PathVariable Long hotelId) {
        hotelService.deleteHotel(hotelId);
    }
}
