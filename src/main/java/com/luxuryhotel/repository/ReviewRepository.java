package com.luxuryhotel.repository;

import com.luxuryhotel.domain.review.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findByRoomIdOrderByCreatedAtDesc(Long roomId, Pageable pageable);

    List<Review> findByRoomIdOrderByCreatedAtDesc(Long roomId);

    Page<Review> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    boolean existsByBookingId(Long bookingId);

    Optional<Review> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.room.id = :roomId")
    Double findAverageRatingByRoomId(@Param("roomId") Long roomId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.room.id = :roomId")
    Long countByRoomId(@Param("roomId") Long roomId);
}
