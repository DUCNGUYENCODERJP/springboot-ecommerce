package com.luxuryhotel.dto.hotel;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HotelRequest(
        @NotBlank(message = "Hotel name is required")
        @Size(max = 150, message = "Hotel name must be at most 150 characters")
        String name,

        @NotBlank(message = "Address is required")
        @Size(max = 255, message = "Address must be at most 255 characters")
        String address,

        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City must be at most 100 characters")
        String city,

        @NotBlank(message = "Country is required")
        @Size(max = 50, message = "Country must be at most 50 characters")
        String country,

        @Min(value = 1, message = "Star rating must be at least 1")
        @Max(value = 5, message = "Star rating must be at most 5")
        Integer starRating,

        @Size(max = 1000, message = "Description must be at most 1000 characters")
        String description
) {
}
