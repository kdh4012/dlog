package com.dlog.post.dto;

import com.dlog.post.Post;

import java.time.LocalDateTime;

public record PostResponse(
        Long id,
        String title,
        String summary,
        String content,
        String category,
        boolean published,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PostResponse from(Post post) {
        return new PostResponse(post.getId(), post.getTitle(), post.getSummary(), post.getContent(),
                post.getCategory(), post.isPublished(), post.getCreatedAt(), post.getUpdatedAt());
    }
}
