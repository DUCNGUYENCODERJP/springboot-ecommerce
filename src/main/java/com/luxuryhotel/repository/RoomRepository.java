package com.luxuryhotel.repository;

import com.luxuryhotel.domain.room.Room;
import com.luxuryhotel.domain.room.RoomStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByHotelId(Long hotelId);

    Page<Room> findByHotelId(Long hotelId, Pageable pageable);

    List<Room> findByStatusAndCapacityGreaterThanEqualAndPricePerNightLessThanEqual(
            RoomStatus status,
            Integer capacity,
            BigDecimal maxPrice
    );

    Optional<Room> findByRoomNumber(String roomNumber);

    boolean existsByRoomNumber(String roomNumber);

    boolean existsByHotelId(Long hotelId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r where r.id = :roomId")
    Optional<Room> findByIdForUpdate(@Param("roomId") Long roomId);

    @Query("""
            select r from Room r
            where r.status = com.luxuryhotel.domain.room.RoomStatus.AVAILABLE
              and r.capacity >= :capacity
              and r.pricePerNight <= :maxPrice
              and (:hotelId is null or r.hotel.id = :hotelId)
              and (:city is null or lower(r.hotel.city) like lower(concat('%', :city, '%')))
              and not exists (
                  select b from Booking b
                  where b.room = r
                    and b.status in :statuses
                    and b.checkInDate < :checkOutDate
                    and b.checkOutDate > :checkInDate
              )
            """)
    List<Room> searchAvailableRooms(
            @Param("hotelId") Long hotelId,
            @Param("city") String city,
            @Param("checkInDate") java.time.LocalDate checkInDate,
            @Param("checkOutDate") java.time.LocalDate checkOutDate,
            @Param("capacity") int capacity,
            @Param("maxPrice") java.math.BigDecimal maxPrice,
            @Param("statuses") List<com.luxuryhotel.domain.booking.BookingStatus> statuses
    );
}
