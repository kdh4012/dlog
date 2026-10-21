package com.dlog.asset.sync.toss;

import com.dlog.asset.AssetSource;
import com.dlog.asset.AssetType;
import com.dlog.asset.Currency;
import com.dlog.asset.sync.ExternalApiException;
import com.dlog.asset.sync.HoldingProvider;
import com.dlog.asset.sync.SyncedHolding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** 토스증권 보유 주식 조회 (국내·미국). 주문 API 는 호출하지 않는다. 토큰은 TossApiClient 가 관리한다 */
@Slf4j
@Component
public class TossHoldingProvider implements HoldingProvider {

    private static final String ACCOUNT_HEADER = "X-Tossinvest-Account";
    private static final String ACCOUNT_LABEL = "토스증권";

    private final TossApiClient api;

    public TossHoldingProvider(TossApiClient api) {
        this.api = api;
    }

    @Override
    public AssetSource source() {
        return AssetSource.TOSS;
    }

    @Override
    public boolean isConfigured() {
        return api.isConfigured();
    }

    @Override
    public synchronized List<SyncedHolding> fetchHoldings() {
        List<TossDtos.Account> accounts = get("토스증권 계좌 목록 조회",
                c -> c.get().uri("/api/v1/accounts"),
                new ParameterizedTypeReference<TossApiClient.Envelope<List<TossDtos.Account>>>() {
                });

        List<SyncedHolding> holdings = new ArrayList<>();
        for (TossDtos.Account account : accounts) {
            TossDtos.HoldingsOverview overview = get("토스증권 보유 주식 조회",
                    c -> c.get().uri("/api/v1/holdings").header(ACCOUNT_HEADER, String.valueOf(account.accountSeq())),
                    new ParameterizedTypeReference<TossApiClient.Envelope<TossDtos.HoldingsOverview>>() {
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

    /** 401 외 4xx 도 동기화 실패로 다룬다 */
    private <T> T get(String action,
                      Function<RestClient, RestClient.RequestHeadersSpec<?>> request,
                      ParameterizedTypeReference<TossApiClient.Envelope<T>> type) {
        try {
            return api.get(action, request, type);
        } catch (HttpClientErrorException e) {
            throw ExternalApiException.of(action, e);
        }
    }
}
