package com.dlog.study.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 여러 카드를 한 번에 추가 (JSON 파일 가져오기) */
public record StudyCardImportRequest(
        @NotEmpty @Size(max = 500) List<@Valid StudyCardRequest> cards
) {
}
