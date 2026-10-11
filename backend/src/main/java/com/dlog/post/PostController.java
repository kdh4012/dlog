package com.dlog.post;

import com.dlog.global.security.SecurityUtils;
import com.dlog.global.web.PageResponse;
import com.dlog.post.dto.PostRequest;
import com.dlog.post.dto.PostResponse;
import com.dlog.post.dto.PostSummaryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @GetMapping
    public PageResponse<PostSummaryResponse> getPosts(
            @RequestParam(required = false) String category,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        // 관리자에게만 비공개(임시저장) 글까지 노출
        return postService.getPosts(category, SecurityUtils.isAdmin(), pageable);
    }

    @GetMapping("/{id}")
    public PostResponse getPost(@PathVariable Long id) {
        return postService.getPost(id, SecurityUtils.isAdmin());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse create(@Valid @RequestBody PostRequest request) {
        return postService.create(request);
    }

    @PutMapping("/{id}")
    public PostResponse update(@PathVariable Long id, @Valid @RequestBody PostRequest request) {
        return postService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        postService.delete(id);
    }
}
