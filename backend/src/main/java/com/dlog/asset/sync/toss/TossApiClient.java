package com.dlog.asset.sync.toss;

import com.dlog.asset.sync.ExternalApiClients;
import com.dlog.asset.sync.ExternalApiException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.function.Function;

/**
 * 토스증권 Open API 공통 호출 (토큰 관리 포함). 보유 자산 동기화와 관심 종목 시세가 함께 쓴다.
 * <p>
 * client 당 유효한 토큰은 1개이고 재발급 시 이전 토큰이 즉시 무효화되므로,
 * 토큰은 반드시 이 빈 하나에서만 발급·보관하고 401 을 받았을 때만 한 번 재발급한다.
 */
@Component
public class TossApiClient {

    /** 만료 직전 토큰 사용을 피하기 위한 여유 시간(초) */
    private static final long EXPIRY_LEEWAY_SECONDS = 60;

    private final TossProperties properties;
    private final RestClient client;

    private String accessToken;
    private Instant tokenExpiresAt = Instant.EPOCH;

    /** 공통 성공 envelope */
    public record Envelope<T>(T result) {
    }

    public TossApiClient(TossProperties properties) {
        this.properties = properties;
        this.client = ExternalApiClients.create(properties.baseUrl());
    }

    public boolean isConfigured() {
        return properties.isConfigured();
    }

    /**
     * 인증 헤더를 붙여 GET 호출하고 result 를 돌려준다. 401 이면 토큰을 재발급해 한 번만 재시도한다.
     * 401 이 아닌 4xx 는 호출부가 상태 코드로 분기할 수 있도록 HttpClientErrorException 그대로 던진다.
     *
     * @throws HttpClientErrorException 401 외 4xx (없는 종목 404 등)
     * @throws ExternalApiException     토큰 발급 실패, 네트워크 오류, 5xx, 재시도 후에도 401
     */
    public synchronized <T> T get(String action,
                                  Function<RestClient, RestClient.RequestHeadersSpec<?>> request,
                                  ParameterizedTypeReference<Envelope<T>> type) {
        try {
            return call(request, type);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() != HttpStatus.UNAUTHORIZED) {
                throw e;
            }
            accessToken = null;
            try {
                return call(request, type);
            } catch (RestClientException retry) {
                throw ExternalApiException.of(action, retry);
            }
        } catch (RestClientException e) {
            throw ExternalApiException.of(action, e);
        }
    }

    private <T> T call(Function<RestClient, RestClient.RequestHeadersSpec<?>> request,
                       ParameterizedTypeReference<Envelope<T>> type) {
        Envelope<T> body = request.apply(client)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token())
                .retrieve()
                .body(type);
        if (body == null || body.result() == null) {
            throw new ExternalApiException("토스증권 응답에 result 가 없습니다.");
        }
        return body.result();
    }

    private String token() {
        Instant now = Instant.now();
        if (accessToken != null && now.isBefore(tokenExpiresAt)) {
            return accessToken;
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        TossDtos.Token issued;
        try {
            issued = client.post()
                    .uri("/oauth2/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TossDtos.Token.class);
        } catch (RestClientException e) {
            throw ExternalApiException.of("토스증권 토큰 발급", e);
        }
        if (issued == null || issued.accessToken() == null) {
            throw new ExternalApiException("토스증권 토큰 발급 응답에 access_token 이 없습니다.");
        }
        accessToken = issued.accessToken();
        tokenExpiresAt = now.plusSeconds(Math.max(0, issued.expiresIn() - EXPIRY_LEEWAY_SECONDS));
        return accessToken;
    }
}
