package com.luxuryhotel.dto.review;

public record RoomReviewSummary(
        Long roomId,
        String roomNumber,
        Double averageRating,
        Long totalReviews
) {
}
