package com.dlog.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * publishedOnly=true 면 공개 글만, category 가 null 이면 전체 카테고리 조회.
     */
    @Query("""
            select p from Post p
            where (:publishedOnly = false or p.published = true)
              and (:category is null or p.category = :category)
            """)
    Page<Post> search(@Param("publishedOnly") boolean publishedOnly,
                      @Param("category") String category,
                      Pageable pageable);

    Optional<Post> findByIdAndPublishedTrue(Long id);
}
