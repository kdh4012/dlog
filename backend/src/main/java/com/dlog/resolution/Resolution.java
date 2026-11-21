package com.dlog.resolution;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 홈 화면의 다짐. 하나만 두므로 id 는 항상 1 */
@Getter
@Entity
@Table(name = "resolution")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Resolution {

    public static final long SINGLE_ID = 1L;

    @Id
    private Long id;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Resolution(String content) {
        this.id = SINGLE_ID;
        change(content);
    }

    public void change(String content) {
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }
}
