package com.luxuryhotel.mapper;

import com.luxuryhotel.domain.review.Review;
import com.luxuryhotel.dto.review.ReviewResponse;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

    public ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getBooking().getId(),
                review.getBooking().getBookingCode(),
                review.getRoom().getId(),
                review.getRoom().getRoomNumber(),
                review.getUser().getId(),
                review.getUser().getFullName(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
