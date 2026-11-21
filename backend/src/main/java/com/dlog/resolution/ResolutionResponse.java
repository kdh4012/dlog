package com.dlog.resolution;

import java.time.LocalDateTime;

/** 아직 적지 않았으면 content 는 빈 문자열, updatedAt 은 null */
public record ResolutionResponse(String content, LocalDateTime updatedAt) {

    static ResolutionResponse empty() {
        return new ResolutionResponse("", null);
    }

    static ResolutionResponse from(Resolution r) {
        return new ResolutionResponse(r.getContent(), r.getUpdatedAt());
    }
}
