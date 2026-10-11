package com.dlog.asset.fx;

import com.dlog.asset.Currency;
import com.dlog.asset.sync.ExternalApiClients;
import com.dlog.asset.sync.ExternalApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 원화·달러 환산용 환율. Frankfurter(ECB 고시 환율, 무료·키 불필요)에서 USD 기준 환율을 받아
 * 1시간 캐시한다. 조회 실패 시 마지막 성공 값을 계속 쓰고, 한 번도 성공 못 했으면 empty 를 돌려
 * 화면은 환산 없이 통화별 금액만 보여준다.
 */
@Slf4j
@Service
public class ExchangeRateService {

    private static final Duration CACHE_TTL = Duration.ofHours(1);
    /** 실패 직후 매 요청마다 외부 호출이 반복되지 않도록 재시도 간격을 둔다 */
    private static final Duration RETRY_INTERVAL = Duration.ofMinutes(5);

    private final RestClient client;

    private ExchangeRates cached;
    private Instant nextFetchAt = Instant.EPOCH;

    public ExchangeRateService(@Value("${dlog.fx.base-url:https://api.frankfurter.dev}") String baseUrl) {
        this.client = ExternalApiClients.create(baseUrl);
    }

    public synchronized Optional<ExchangeRates> current() {
        Instant now = Instant.now();
        if (now.isBefore(nextFetchAt)) {
            return Optional.ofNullable(cached);
        }
        try {
            cached = fetch();
            nextFetchAt = now.plus(CACHE_TTL);
        } catch (ExternalApiException e) {
            log.warn("환율 조회 실패, {} 사용: {}", cached == null ? "환산 생략" : cached.date() + " 환율", e.getMessage());
            nextFetchAt = now.plus(RETRY_INTERVAL);
        }
        return Optional.ofNullable(cached);
    }

    /** USD 기준 환율(1 USD = n 통화)을 1 단위당 원화로 바꾼다 */
    private ExchangeRates fetch() {
        String symbols = Arrays.stream(Currency.values())
                .filter(c -> c != Currency.USD)
                .map(Enum::name)
                .collect(Collectors.joining(","));
        FrankfurterResponse body;
        try {
            body = client.get()
                    .uri(b -> b.path("/v1/latest").queryParam("base", "USD").queryParam("symbols", symbols).build())
                    .retrieve()
                    .body(FrankfurterResponse.class);
        } catch (RestClientException e) {
            throw ExternalApiException.of("환율 조회", e);
        }
        if (body == null || body.rates() == null || body.rates().get("KRW") == null) {
            throw new ExternalApiException("환율 응답에 KRW 가 없습니다.");
        }

        BigDecimal krwPerUsd = body.rates().get("KRW");
        Map<Currency, BigDecimal> krwPerUnit = new EnumMap<>(Currency.class);
        krwPerUnit.put(Currency.KRW, BigDecimal.ONE);
        krwPerUnit.put(Currency.USD, krwPerUsd);
        for (Currency c : Currency.values()) {
            BigDecimal perUsd = body.rates().get(c.name());
            if (c != Currency.KRW && c != Currency.USD && perUsd != null && perUsd.signum() > 0) {
                // 1 JPY = (KRW/USD) / (JPY/USD) 원
                krwPerUnit.put(c, krwPerUsd.divide(perUsd, MathContext.DECIMAL64));
            }
        }
        return new ExchangeRates(body.date(), krwPerUnit);
    }

    /** GET /v1/latest?base=USD&symbols=KRW,JPY,EUR */
    record FrankfurterResponse(LocalDate date, Map<String, BigDecimal> rates) {
    }
}
