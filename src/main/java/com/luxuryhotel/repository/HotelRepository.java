package com.luxuryhotel.repository;

import com.luxuryhotel.domain.hotel.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HotelRepository extends JpaRepository<Hotel, Long> {
    Page<Hotel> findAll(Pageable pageable);
}
