package com.luxuryhotel.controller;

import com.luxuryhotel.dto.common.PageResponse;
import com.luxuryhotel.dto.room.RoomRequest;
import com.luxuryhotel.dto.room.RoomResponse;
import com.luxuryhotel.service.room.RoomService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @GetMapping
    public List<RoomResponse> getAllRooms(@RequestParam(required = false) Long hotelId) {
        return roomService.getAllRooms(hotelId);
    }

    @GetMapping("/page")
    public PageResponse<RoomResponse> getRoomsPage(@RequestParam(required = false) Long hotelId,
                                                   Pageable pageable) {
        return roomService.getRoomsPage(hotelId, pageable);
    }

    @GetMapping("/{roomId}")
    public RoomResponse getRoomById(@PathVariable Long roomId) {
        return roomService.getRoomById(roomId);
    }

    @GetMapping("/available")
    public List<RoomResponse> searchAvailableRooms(@RequestParam LocalDate checkInDate,
                                                   @RequestParam LocalDate checkOutDate,
                                                   @RequestParam(required = false) String city,
                                                   @RequestParam(required = false) Long hotelId,
                                                   @RequestParam(required = false) Integer guestCount,
                                                   @RequestParam(required = false) BigDecimal maxPrice) {
        return roomService.searchAvailableRooms(hotelId, city, checkInDate, checkOutDate, guestCount, maxPrice);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public RoomResponse createRoom(@Valid @RequestBody RoomRequest request) {
        return roomService.createRoom(request);
    }

    @PutMapping("/{roomId}")
    @PreAuthorize("hasRole('ADMIN')")
    public RoomResponse updateRoom(@PathVariable Long roomId, @Valid @RequestBody RoomRequest request) {
        return roomService.updateRoom(roomId, request);
    }

    @DeleteMapping("/{roomId}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRoom(@PathVariable Long roomId) {
        roomService.deleteRoom(roomId);
    }
}
