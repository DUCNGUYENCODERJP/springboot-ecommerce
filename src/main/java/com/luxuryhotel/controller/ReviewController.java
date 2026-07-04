package com.luxuryhotel.controller;

import com.luxuryhotel.dto.common.PageResponse;
import com.luxuryhotel.dto.review.CreateReviewRequest;
import com.luxuryhotel.dto.review.ReviewResponse;
import com.luxuryhotel.dto.review.RoomReviewSummary;
import com.luxuryhotel.service.review.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * Tạo đánh giá mới (chỉ user đã CHECKED_OUT mới được phép)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse createReview(@Valid @RequestBody CreateReviewRequest request) {
        return reviewService.createReview(request);
    }

    /**
     * Lấy danh sách đánh giá của 1 phòng — công khai, không cần đăng nhập
     */
    @GetMapping("/room/{roomId}")
    public PageResponse<ReviewResponse> getReviewsByRoom(
            @PathVariable Long roomId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return reviewService.getReviewsByRoom(roomId, pageable);
    }

    /**
     * Lấy thống kê đánh giá (điểm trung bình, số lượng) của 1 phòng — công khai
     */
    @GetMapping("/room/{roomId}/summary")
    public RoomReviewSummary getRoomReviewSummary(@PathVariable Long roomId) {
        return reviewService.getRoomReviewSummary(roomId);
    }

    /**
     * Lấy đánh giá của người dùng hiện tại
     */
    @GetMapping("/my")
    public PageResponse<ReviewResponse> getMyReviews(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return reviewService.getMyReviews(pageable);
    }

    /**
     * Xóa đánh giá — chỉ chủ sở hữu hoặc ADMIN
     */
    @DeleteMapping("/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
    }
}
