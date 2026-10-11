package com.dlog.post;

import com.dlog.global.error.BusinessException;
import com.dlog.global.error.ErrorCode;
import com.dlog.global.web.PageResponse;
import com.dlog.post.dto.PostRequest;
import com.dlog.post.dto.PostResponse;
import com.dlog.post.dto.PostSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;

    public PageResponse<PostSummaryResponse> getPosts(String category, boolean includeDrafts, Pageable pageable) {
        String categoryFilter = StringUtils.hasText(category) ? category : null;
        return PageResponse.from(
                postRepository.search(!includeDrafts, categoryFilter, pageable).map(PostSummaryResponse::from));
    }

    public PostResponse getPost(Long id, boolean includeDrafts) {
        Post post = (includeDrafts ? postRepository.findById(id) : postRepository.findByIdAndPublishedTrue(id))
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        return PostResponse.from(post);
    }

    @Transactional
    public PostResponse create(PostRequest request) {
        Post post = Post.builder()
                .title(request.title())
                .summary(request.summary())
                .content(request.content())
                .category(request.category())
                .published(request.published())
                .build();
        return PostResponse.from(postRepository.save(post));
    }

    @Transactional
    public PostResponse update(Long id, PostRequest request) {
        Post post = findById(id);
        post.update(request.title(), request.summary(), request.content(), request.category(), request.published());
        postRepository.flush(); // updatedAt 반영
        return PostResponse.from(post);
    }

    @Transactional
    public void delete(Long id) {
        postRepository.delete(findById(id));
    }

    private Post findById(Long id) {
        return postRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
    }
}
