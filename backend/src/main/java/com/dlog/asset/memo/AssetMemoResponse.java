package com.dlog.asset.memo;

import java.time.LocalDateTime;

/** 아직 적지 않았으면 content 는 빈 문자열, updatedAt 은 null */
public record AssetMemoResponse(String content, LocalDateTime updatedAt) {

    static AssetMemoResponse empty() {
        return new AssetMemoResponse("", null);
    }

    static AssetMemoResponse from(AssetMemo m) {
        return new AssetMemoResponse(m.getContent(), m.getUpdatedAt());
    }
}
