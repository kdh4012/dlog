package com.dlog.asset.sync;

import org.springframework.web.client.RestClientResponseException;

/** 외부 자산 API 호출 실패. 메시지는 동기화 상태로 화면에 그대로 노출된다 */
public class ExternalApiException extends RuntimeException {

    private static final int MAX_BODY_LENGTH = 300;

    public ExternalApiException(String message) {
        super(message);
    }

    public ExternalApiException(String message, Throwable cause) {
        super(message, cause);
    }

    /** HTTP 오류 응답이면 상태 코드와 응답 본문(허용 IP 미등록 등 원인 포함)을 메시지에 붙인다 */
    public static ExternalApiException of(String action, Exception e) {
        if (e instanceof RestClientResponseException re) {
            String body = re.getResponseBodyAsString();
            if (body.length() > MAX_BODY_LENGTH) {
                body = body.substring(0, MAX_BODY_LENGTH) + "...";
            }
            return new ExternalApiException("%s 실패 (HTTP %d) %s".formatted(action, re.getStatusCode().value(), body), e);
        }
        return new ExternalApiException("%s 실패: %s".formatted(action, e.getMessage()), e);
    }
}
