package com.dlog.global.error;

import java.util.List;

public record ErrorResponse(String code, String message, List<FieldError> errors) {

    public record FieldError(String field, String reason) {
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, List<FieldError> errors) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage(), errors);
    }
}
