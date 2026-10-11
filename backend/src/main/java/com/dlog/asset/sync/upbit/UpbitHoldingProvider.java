package com.dlog.asset.sync.upbit;

import com.dlog.asset.AssetSource;
import com.dlog.asset.AssetType;
import com.dlog.asset.Currency;
import com.dlog.asset.sync.ExternalApiClients;
import com.dlog.asset.sync.ExternalApiException;
import com.dlog.asset.sync.HoldingProvider;
import com.dlog.asset.sync.SyncedHolding;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 업비트 보유 자산 조회. 원화 잔고는 현금, 코인은 KRW 마켓 현재가로 평가한다.
 * 주문·출금 API 는 호출하지 않는다.
 */
@Slf4j
@Component
public class UpbitHoldingProvider implements HoldingProvider {

    private static final String KRW = "KRW";
    private static final String ACCOUNT_LABEL = "업비트";

    private final UpbitProperties properties;
    private final RestClient client;

    public UpbitHoldingProvider(UpbitProperties properties) {
        this.properties = properties;
        this.client = ExternalApiClients.create(properties.baseUrl());
    }

    @Override
    public AssetSource source() {
        return AssetSource.UPBIT;
    }

    @Override
    public boolean isConfigured() {
        return properties.isConfigured();
    }

    @Override
    public List<SyncedHolding> fetchHoldings() {
        List<UpbitDtos.Account> accounts = fetchAccounts();
        List<SyncedHolding> holdings = new ArrayList<>();

        accounts.stream()
                .filter(a -> KRW.equals(a.currency()))
                .map(UpbitHoldingProvider::totalQuantity)
                .filter(q -> q.signum() > 0)
                .findFirst()
                .ifPresent(krw -> holdings.add(new SyncedHolding(AssetType.CASH, "원화 예수금", KRW, ACCOUNT_LABEL,
                        krw, BigDecimal.ONE, BigDecimal.ONE, Currency.KRW)));

        List<UpbitDtos.Account> coins = accounts.stream()
                .filter(a -> !KRW.equals(a.currency()))
                .filter(a -> totalQuantity(a).signum() > 0)
                .toList();
        if (coins.isEmpty()) {
            return holdings;
        }

        // 존재하지 않는 마켓이 하나라도 섞이면 시세 조회 전체가 실패하므로 KRW 마켓 목록으로 먼저 거른다
        Map<String, String> krwMarketNames = fetchKrwMarketNames();
        List<UpbitDtos.Account> tradable = new ArrayList<>();
        for (UpbitDtos.Account coin : coins) {
            if (KRW.equals(coin.unitCurrency()) && krwMarketNames.containsKey(marketCode(coin))) {
                tradable.add(coin);
            } else {
                log.warn("업비트 KRW 마켓이 없어 동기화에서 제외: currency={}, unitCurrency={}",
                        coin.currency(), coin.unitCurrency());
            }
        }
        if (tradable.isEmpty()) {
            return holdings;
        }

        Map<String, BigDecimal> prices = fetchPrices(tradable.stream().map(UpbitHoldingProvider::marketCode).toList());
        for (UpbitDtos.Account coin : tradable) {
            String market = marketCode(coin);
            BigDecimal price = prices.get(market);
            if (price == null) {
                throw new ExternalApiException("업비트 현재가 응답에 " + market + " 이(가) 없습니다.");
            }
            holdings.add(new SyncedHolding(AssetType.CRYPTO, krwMarketNames.get(market), coin.currency(),
                    ACCOUNT_LABEL, totalQuantity(coin), decimal(coin.avgBuyPrice()), price, Currency.KRW));
        }
        return holdings;
    }

    private List<UpbitDtos.Account> fetchAccounts() {
        try {
            List<UpbitDtos.Account> body = client.get()
                    .uri("/v1/accounts")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + createToken())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<UpbitDtos.Account>>() {
                    });
            return body == null ? List.of() : body;
        } catch (RestClientException e) {
            throw ExternalApiException.of("업비트 자산 조회", e);
        }
    }

    private Map<String, String> fetchKrwMarketNames() {
        try {
            List<UpbitDtos.Market> markets = client.get()
                    .uri("/v1/market/all")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<UpbitDtos.Market>>() {
                    });
            return (markets == null ? List.<UpbitDtos.Market>of() : markets).stream()
                    .filter(m -> m.market().startsWith(KRW + "-"))
                    .collect(Collectors.toMap(UpbitDtos.Market::market, UpbitDtos.Market::koreanName));
        } catch (RestClientException e) {
            throw ExternalApiException.of("업비트 마켓 목록 조회", e);
        }
    }

    private Map<String, BigDecimal> fetchPrices(List<String> markets) {
        try {
            List<UpbitDtos.Ticker> tickers = client.get()
                    .uri(b -> b.path("/v1/ticker").queryParam("markets", String.join(",", markets)).build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<UpbitDtos.Ticker>>() {
                    });
            return (tickers == null ? List.<UpbitDtos.Ticker>of() : tickers).stream()
                    .collect(Collectors.toMap(UpbitDtos.Ticker::market, t -> decimal(t.tradePrice()),
                            (a, b) -> a));
        } catch (RestClientException e) {
            throw ExternalApiException.of("업비트 현재가 조회", e);
        }
    }

    /**
     * 쿼리 파라미터가 없는 조회 API 는 access_key + nonce 만 HS256 으로 서명한다.
     * nonce 는 요청마다 달라야 재사용 거부를 피한다.
     */
    private String createToken() {
        return Jwts.builder()
                .claim("access_key", properties.accessKey())
                .claim("nonce", UUID.randomUUID().toString())
                .signWith(Keys.hmacShaKeyFor(properties.secretKey().getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();
    }

    private static String marketCode(UpbitDtos.Account coin) {
        return KRW + "-" + coin.currency();
    }

    /** 주문 가능 수량 + 주문 중 묶인 수량 */
    private static BigDecimal totalQuantity(UpbitDtos.Account a) {
        return decimal(a.balance()).add(decimal(a.locked()));
    }

    private static BigDecimal decimal(String value) {
        return value == null || value.isBlank() ? BigDecimal.ZERO : new BigDecimal(value);
    }
}
