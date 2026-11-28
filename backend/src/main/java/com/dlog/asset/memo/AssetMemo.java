package com.dlog.asset.memo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 자산 포트폴리오 메모 (예: 'SOXL 1억 되면 일부 팔아 채권 매수'). 하나만 두므로 id 는 항상 1 */
@Getter
@Entity
@Table(name = "asset_memo")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssetMemo {

    public static final long SINGLE_ID = 1L;

    @Id
    private Long id;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public AssetMemo(String content) {
        this.id = SINGLE_ID;
        change(content);
    }

    public void change(String content) {
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }
}
