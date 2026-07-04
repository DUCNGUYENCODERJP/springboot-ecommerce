package com.luxuryhotel.service.review;

import com.luxuryhotel.domain.booking.Booking;
import com.luxuryhotel.domain.booking.BookingStatus;
import com.luxuryhotel.domain.review.Review;
import com.luxuryhotel.domain.user.Role;
import com.luxuryhotel.domain.user.User;
import com.luxuryhotel.dto.common.PageResponse;
import com.luxuryhotel.dto.review.CreateReviewRequest;
import com.luxuryhotel.dto.review.ReviewResponse;
import com.luxuryhotel.dto.review.RoomReviewSummary;
import com.luxuryhotel.mapper.ReviewMapper;
import com.luxuryhotel.repository.BookingRepository;
import com.luxuryhotel.repository.ReviewRepository;
import com.luxuryhotel.repository.RoomRepository;
import com.luxuryhotel.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final UserService userService;
    private final ReviewMapper reviewMapper;

    @Transactional
    public ReviewResponse createReview(CreateReviewRequest request) {
        User currentUser = userService.getCurrentUserEntity();

        Booking booking = bookingRepository.findByIdAndUserId(request.bookingId(), currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Booking not found or does not belong to you"));

        if (booking.getStatus() != BookingStatus.CHECKED_OUT) {
            throw new IllegalArgumentException("You can only review rooms after check-out (booking status must be CHECKED_OUT)");
        }

        if (reviewRepository.existsByBookingId(booking.getId())) {
            throw new IllegalArgumentException("You have already submitted a review for this booking");
        }

        Review review = new Review();
        review.setBooking(booking);
        review.setRoom(booking.getRoom());
        review.setUser(currentUser);
        review.setRating(request.rating());
        review.setComment(request.comment() == null ? null : request.comment().trim());

        return reviewMapper.toResponse(reviewRepository.save(review));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getReviewsByRoom(Long roomId, Pageable pageable) {
        if (!roomRepository.existsById(roomId)) {
            throw new IllegalArgumentException("Room not found");
        }
        return PageResponse.from(
                reviewRepository.findByRoomIdOrderByCreatedAtDesc(roomId, pageable)
                        .map(reviewMapper::toResponse)
        );
    }

    @Transactional(readOnly = true)
    public RoomReviewSummary getRoomReviewSummary(Long roomId) {
        if (!roomRepository.existsById(roomId)) {
            throw new IllegalArgumentException("Room not found");
        }
        Double average = reviewRepository.findAverageRatingByRoomId(roomId);
        Long total = reviewRepository.countByRoomId(roomId);
        String roomNumber = roomRepository.findById(roomId)
                .map(r -> r.getRoomNumber())
                .orElse("");
        return new RoomReviewSummary(roomId, roomNumber, average != null ? Math.round(average * 10.0) / 10.0 : null, total);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getMyReviews(Pageable pageable) {
        User currentUser = userService.getCurrentUserEntity();
        return PageResponse.from(
                reviewRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId(), pageable)
                        .map(reviewMapper::toResponse)
        );
    }

    @Transactional
    public void deleteReview(Long reviewId) {
        User currentUser = userService.getCurrentUserEntity();
        boolean isAdmin = currentUser.getRoles().contains(Role.ADMIN);

        Review review;
        if (isAdmin) {
            review = reviewRepository.findById(reviewId)
                    .orElseThrow(() -> new IllegalArgumentException("Review not found"));
        } else {
            review = reviewRepository.findByIdAndUserId(reviewId, currentUser.getId())
                    .orElseThrow(() -> new AccessDeniedException("Review not found or you are not the author"));
        }

        reviewRepository.delete(review);
    }
}
