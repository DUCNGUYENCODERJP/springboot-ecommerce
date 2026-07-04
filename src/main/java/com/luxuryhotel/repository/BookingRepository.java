package com.luxuryhotel.repository;

import com.luxuryhotel.domain.booking.Booking;
import com.luxuryhotel.domain.booking.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserId(Long userId);

    Page<Booking> findByUserId(Long userId, Pageable pageable);

    Optional<Booking> findByIdAndUserId(Long id, Long userId);

    boolean existsByRoomId(Long roomId);

    boolean existsByRoomIdAndStatusInAndCheckInDateLessThanAndCheckOutDateGreaterThan(
            Long roomId,
            List<BookingStatus> statuses,
            LocalDate checkOutDate,
            LocalDate checkInDate
    );

    @Query("""
            select b from Booking b
            where (:hotelId is null or b.room.hotel.id = :hotelId)
            order by b.createdAt desc
            """)
    List<Booking> findAllByHotelId(@Param("hotelId") Long hotelId);

    @Query("""
            select b from Booking b
            where (:hotelId is null or b.room.hotel.id = :hotelId)
            order by b.createdAt desc
            """)
    Page<Booking> findAllByHotelId(@Param("hotelId") Long hotelId, Pageable pageable);

    @Query("""
            select distinct b from Booking b
            join fetch b.user
            join fetch b.room r
            join fetch r.hotel
            order by b.createdAt desc
            """)
    List<Booking> findAllWithDetailsOrderByCreatedAtDesc();
}
