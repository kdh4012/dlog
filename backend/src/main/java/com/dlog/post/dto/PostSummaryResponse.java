package com.dlog.post.dto;

import com.dlog.post.Post;

import java.time.LocalDateTime;

/** 목록용 응답 (본문 제외) */
public record PostSummaryResponse(
        Long id,
        String title,
        String summary,
        String category,
        boolean published,
        LocalDateTime createdAt
) {
    public static PostSummaryResponse from(Post post) {
        return new PostSummaryResponse(post.getId(), post.getTitle(), post.getSummary(),
                post.getCategory(), post.isPublished(), post.getCreatedAt());
    }
}
