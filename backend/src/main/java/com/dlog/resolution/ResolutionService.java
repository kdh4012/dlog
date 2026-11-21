package com.dlog.resolution;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResolutionService {

    private final ResolutionRepository repository;

    public ResolutionResponse get() {
        return repository.findById(Resolution.SINGLE_ID)
                .map(ResolutionResponse::from)
                .orElseGet(ResolutionResponse::empty);
    }

    /** 공백만 있으면 지운다 */
    @Transactional
    public ResolutionResponse save(String content) {
        String text = content.strip();
        if (text.isEmpty()) {
            repository.deleteById(Resolution.SINGLE_ID);
            return ResolutionResponse.empty();
        }
        Resolution resolution = repository.findById(Resolution.SINGLE_ID)
                .map(r -> {
                    r.change(text);
                    return r;
                })
                .orElseGet(() -> repository.save(new Resolution(text)));
        return ResolutionResponse.from(resolution);
    }
}
