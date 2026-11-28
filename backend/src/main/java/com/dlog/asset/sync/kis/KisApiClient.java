package com.dlog.asset.sync.kis;

import com.dlog.asset.sync.ExternalApiClients;
import com.dlog.asset.sync.ExternalApiException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.Map;

/**
 * 한국투자증권 Open API 공통 호출 (토큰 관리 포함).
 * <p>
 * 접근토큰은 24시간 유효하고 발급 횟수 제한(1분 1회)이 있으므로 만료 전까지 재사용한다.
 * 응답은 HTTP 200 이어도 rt_cd 가 "0" 이 아니면 실패다.
 */
@Component
public class KisApiClient {

    /** 만료 직전 토큰 사용을 피하기 위한 여유 시간(초) */
    private static final long EXPIRY_LEEWAY_SECONDS = 600;

    private final KisProperties properties;
    private final RestClient client;

    private String accessToken;
    private Instant tokenExpiresAt = Instant.EPOCH;

    public KisApiClient(KisProperties properties) {
        this.properties = properties;
        this.client = ExternalApiClients.create(properties.baseUrl());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Token(@JsonProperty("access_token") String accessToken, @JsonProperty("expires_in") long expiresIn) {
    }

    /** 공통 응답 머리. 각 조회 응답 DTO 가 함께 가진다 */
    public interface Result {
        String rtCd();

        String msgCd();

        String msg1();
    }

    /** 응답 본문과 연속조회 여부(tr_cont 헤더 F/M 이면 다음 페이지 있음) */
    public record Page<T>(T body, boolean hasNext) {
    }

    /**
     * GET 조회. 실패(HTTP 오류·rt_cd≠0)는 ExternalApiException.
     *
     * @param trCont 첫 페이지는 "", 다음 페이지는 "N"
     */
    public synchronized <T extends Result> Page<T> get(String action, String path, String trId, String trCont,
                                                       Map<String, String> params, Class<T> type) {
        ResponseEntity<T> response;
        try {
            response = client.get()
                    .uri(b -> {
                        b.path(path);
                        params.forEach(b::queryParam);
                        return b.build();
                    })
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
                    .header("appkey", properties.appKey())
                    .header("appsecret", properties.appSecret())
                    .header("tr_id", trId)
                    .header("tr_cont", trCont)
                    .header("custtype", "P")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toEntity(type);
        } catch (RestClientException e) {
            // 토큰이 만료·폐기됐을 수 있으니 다음 동기화에서 새로 발급받는다
            accessToken = null;
            throw ExternalApiException.of(action, e);
        }
        T body = response.getBody();
        if (body == null) {
            throw new ExternalApiException(action + " 응답이 비어 있습니다.");
        }
        if (!"0".equals(body.rtCd())) {
            throw new ExternalApiException("%s 실패: [%s] %s".formatted(action, body.msgCd(), body.msg1()));
        }
        String next = response.getHeaders().getFirst("tr_cont");
        return new Page<>(body, "F".equals(next) || "M".equals(next));
    }

    private String token() {
        Instant now = Instant.now();
        if (accessToken != null && now.isBefore(tokenExpiresAt)) {
            return accessToken;
        }
        Token issued;
        try {
            issued = client.post()
                    .uri("/oauth2/tokenP")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "grant_type", "client_credentials",
                            "appkey", properties.appKey(),
                            "appsecret", properties.appSecret()))
                    .retrieve()
                    .body(Token.class);
        } catch (RestClientException e) {
            throw ExternalApiException.of("한국투자증권 토큰 발급", e);
        }
        if (issued == null || issued.accessToken() == null) {
            throw new ExternalApiException("한국투자증권 토큰 발급 응답에 access_token 이 없습니다.");
        }
        accessToken = issued.accessToken();
        tokenExpiresAt = now.plusSeconds(Math.max(0, issued.expiresIn() - EXPIRY_LEEWAY_SECONDS));
        return accessToken;
    }
}
