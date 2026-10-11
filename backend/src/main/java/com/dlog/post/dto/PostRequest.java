package com.dlog.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 500) String summary,
        @NotBlank String content,
        @Size(max = 50) String category,
        boolean published
) {
}
