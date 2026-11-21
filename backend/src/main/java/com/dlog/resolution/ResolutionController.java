package com.dlog.resolution;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 홈 화면 다짐 API - SecurityConfig 에서 관리자 전용으로 제한 */
@RestController
@RequestMapping("/api/resolution")
@RequiredArgsConstructor
public class ResolutionController {

    private final ResolutionService resolutionService;

    @GetMapping
    public ResolutionResponse get() {
        return resolutionService.get();
    }

    @PutMapping
    public ResolutionResponse save(@Valid @RequestBody ResolutionRequest request) {
        return resolutionService.save(request.content());
    }
}
