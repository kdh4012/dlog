package com.dlog.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."),
    WORK_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "작업 기록을 찾을 수 없습니다."),
    ASSET_NOT_FOUND(HttpStatus.NOT_FOUND, "자산을 찾을 수 없습니다."),
    ASSET_SYNCED_READONLY(HttpStatus.CONFLICT, "외부 API 로 동기화되는 자산은 수정·삭제할 수 없습니다."),
    EXPENSE_NOT_FOUND(HttpStatus.NOT_FOUND, "지출 내역을 찾을 수 없습니다."),
    FIXED_EXPENSE_NOT_FOUND(HttpStatus.NOT_FOUND, "고정지출을 찾을 수 없습니다."),
    FIXED_COST_CATEGORY_RESERVED(HttpStatus.BAD_REQUEST, "고정비 카테고리는 고정지출에서만 사용할 수 있습니다."),
    WATCHLIST_NOT_FOUND(HttpStatus.NOT_FOUND, "관심 종목을 찾을 수 없습니다."),
    WATCHLIST_DUPLICATE(HttpStatus.CONFLICT, "이미 관심 종목에 있는 티커입니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;
}
