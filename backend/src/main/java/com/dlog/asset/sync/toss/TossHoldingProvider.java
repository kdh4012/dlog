package com.dlog.asset.sync.toss;

import com.dlog.asset.AssetSource;
import com.dlog.asset.AssetType;
import com.dlog.asset.Currency;
import com.dlog.asset.sync.ExternalApiClients;
import com.dlog.asset.sync.ExternalApiException;
import com.dlog.asset.sync.HoldingProvider;
import com.dlog.asset.sync.SyncedHolding;
import lombok.extern.slf4j.Slf4j;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * 토스증권 보유 주식 조회 (국내·미국). 주문 API 는 호출하지 않는다.
 * <p>
 * client 당 유효한 토큰은 1개이고 재발급 시 이전 토큰이 즉시 무효화되므로,
 * 토큰을 메모리에 보관해 재사용하고 401 을 받았을 때만 한 번 재발급한다.
 */
@Slf4j
@Component
public class TossHoldingProvider implements HoldingProvider {

    private static final String ACCOUNT_HEADER = "X-Tossinvest-Account";
    private static final String ACCOUNT_LABEL = "토스증권";
    /** 만료 직전 토큰 사용을 피하기 위한 여유 시간(초) */
    private static final long EXPIRY_LEEWAY_SECONDS = 60;

    private final TossProperties properties;
    private final RestClient client;

    private String accessToken;
    private Instant tokenExpiresAt = Instant.EPOCH;

    public TossHoldingProvider(TossProperties properties) {
        this.properties = properties;
        this.client = ExternalApiClients.create(properties.baseUrl());
    }

    @Override
    public AssetSource source() {
        return AssetSource.TOSS;
    }

    @Override
    public boolean isConfigured() {
        return properties.isConfigured();
    }

    @Override
    public synchronized List<SyncedHolding> fetchHoldings() {
        List<TossDtos.Account> accounts = get("토스증권 계좌 목록 조회",
                c -> c.get().uri("/api/v1/accounts"),
                new ParameterizedTypeReference<TossDtos.Envelope<List<TossDtos.Account>>>() {
                });

        List<SyncedHolding> holdings = new ArrayList<>();
        for (TossDtos.Account account : accounts) {
            TossDtos.HoldingsOverview overview = get("토스증권 보유 주식 조회",
                    c -> c.get().uri("/api/v1/holdings").header(ACCOUNT_HEADER, String.valueOf(account.accountSeq())),
                    new ParameterizedTypeReference<TossDtos.Envelope<TossDtos.HoldingsOverview>>() {
                    });
            String label = accountLabel(account.accountNo());
            for (TossDtos.HoldingsItem item : overview.items()) {
                toHolding(item, label).ifPresent(holdings::add);
            }
        }
        return holdings;
    }

    private Optional<SyncedHolding> toHolding(TossDtos.HoldingsItem item, String account) {
        // 토스는 미지의 통화 enum 이 추가될 수 있다고 명시하므로 지원 통화만 받는다
        Optional<Currency> currency = Arrays.stream(Currency.values())
                .filter(c -> c.name().equals(item.currency()))
                .findFirst();
        if (currency.isEmpty()) {
            log.warn("지원하지 않는 통화라 동기화에서 제외: symbol={}, currency={}", item.symbol(), item.currency());
            return Optional.empty();
        }
        // 보유 주식 API 는 주식·ETF 를 구분하지 않으므로 STOCK 으로 통일
        return Optional.of(new SyncedHolding(AssetType.STOCK, item.name(), item.symbol(), account,
                new BigDecimal(item.quantity()), new BigDecimal(item.averagePurchasePrice()),
                new BigDecimal(item.lastPrice()), currency.get()));
    }

    /** 계좌번호는 끝 4자리만 남긴다 (예: 토스증권 ··1234) */
    private static String accountLabel(String accountNo) {
        if (accountNo == null || accountNo.length() < 4) {
            return ACCOUNT_LABEL;
        }
        return ACCOUNT_LABEL + " ··" + accountNo.substring(accountNo.length() - 4);
    }

    /** 인증 헤더를 붙여 호출하고, 401 이면 토큰을 재발급해 한 번만 재시도한다 */
    private <T> T get(String action,
                      Function<RestClient, RestClient.RequestHeadersSpec<?>> request,
                      ParameterizedTypeReference<TossDtos.Envelope<T>> type) {
        try {
            return call(request, type);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() != HttpStatus.UNAUTHORIZED) {
                throw ExternalApiException.of(action, e);
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
                       ParameterizedTypeReference<TossDtos.Envelope<T>> type) {
        TossDtos.Envelope<T> body = request.apply(client)
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
