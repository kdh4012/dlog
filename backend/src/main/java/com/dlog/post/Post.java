package com.dlog.post;

import com.dlog.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "post")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 500)
    private String summary;

    /** 마크다운 본문 */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 50)
    private String category;

    @Column(nullable = false)
    private boolean published;

    @Builder
    private Post(String title, String summary, String content, String category, boolean published) {
        this.title = title;
        this.summary = summary;
        this.content = content;
        this.category = category;
        this.published = published;
    }

    public void update(String title, String summary, String content, String category, boolean published) {
        this.title = title;
        this.summary = summary;
        this.content = content;
        this.category = category;
        this.published = published;
    }
}
