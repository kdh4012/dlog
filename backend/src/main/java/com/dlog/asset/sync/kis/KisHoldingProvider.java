package com.dlog.asset.sync.kis;

import com.dlog.asset.AssetSource;
import com.dlog.asset.AssetType;
import com.dlog.asset.Currency;
import com.dlog.asset.sync.HoldingProvider;
import com.dlog.asset.sync.SyncedHolding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 한국투자증권 보유 주식 조회 (국내·미국). 주문 API 는 호출하지 않는다.
 * 금현물 계좌는 Open API 로 조회할 수 없어 금은 직접 입력(GOLD)으로 관리한다.
 */
@Slf4j
@Component
public class KisHoldingProvider implements HoldingProvider {

    /** 한 계좌에서 따라갈 최대 페이지 수 (무한 반복 방지) */
    private static final int MAX_PAGES = 10;

    private final KisProperties properties;
    private final KisApiClient api;

    public KisHoldingProvider(KisProperties properties, KisApiClient api) {
        this.properties = properties;
        this.api = api;
    }

    @Override
    public AssetSource source() {
        return AssetSource.KIS;
    }

    @Override
    public boolean isConfigured() {
        return properties.isConfigured();
    }

    @Override
    public List<SyncedHolding> fetchHoldings() {
        List<SyncedHolding> holdings = new ArrayList<>();
        for (KisProperties.Account account : properties.accountList()) {
            holdings.addAll(domestic(account));
            if (account.canHoldOverseas()) {
                holdings.addAll(overseas(account));
            }
        }
        return holdings;
    }

    private List<SyncedHolding> domestic(KisProperties.Account account) {
        List<SyncedHolding> result = new ArrayList<>();
        String fk = "";
        String nk = "";
        for (int page = 0; page < MAX_PAGES; page++) {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("CANO", account.cano());
            params.put("ACNT_PRDT_CD", account.productCode());
            params.put("AFHR_FLPR_YN", "N");
            params.put("OFL_YN", "");
            params.put("INQR_DVSN", "02"); // 종목별
            params.put("UNPR_DVSN", "01");
            params.put("FUND_STTL_ICLD_YN", "N");
            params.put("FNCG_AMT_AUTO_RDPT_YN", "N");
            params.put("PRCS_DVSN", "00"); // 전일매매 포함
            params.put("CTX_AREA_FK100", fk);
            params.put("CTX_AREA_NK100", nk);
            KisApiClient.Page<KisDtos.DomesticBalance> res = api.get("한국투자 국내 잔고 조회",
                    "/uapi/domestic-stock/v1/trading/inquire-balance", "TTTC8434R", page == 0 ? "" : "N",
                    params, KisDtos.DomesticBalance.class);
            for (KisDtos.DomesticItem item : nullSafe(res.body().output1())) {
                BigDecimal qty = number(item.quantity());
                if (qty.signum() <= 0) {
                    continue; // 당일 전량 매도한 종목도 0주로 내려온다
                }
                result.add(new SyncedHolding(AssetType.STOCK, item.name(), item.pdno(), account.label(),
                        qty, number(item.avgPrice()), number(item.prpr()), Currency.KRW));
            }
            if (!res.hasNext()) {
                break;
            }
            fk = blankToEmpty(res.body().ctxFk());
            nk = blankToEmpty(res.body().ctxNk());
        }
        return result;
    }

    private List<SyncedHolding> overseas(KisProperties.Account account) {
        List<SyncedHolding> result = new ArrayList<>();
        String fk = "";
        String nk = "";
        for (int page = 0; page < MAX_PAGES; page++) {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("CANO", account.cano());
            params.put("ACNT_PRDT_CD", account.productCode());
            params.put("OVRS_EXCG_CD", "NASD"); // 실전투자에서 NASD 는 미국 전체
            params.put("TR_CRCY_CD", "USD");
            params.put("CTX_AREA_FK200", fk);
            params.put("CTX_AREA_NK200", nk);
            KisApiClient.Page<KisDtos.OverseasBalance> res = api.get("한국투자 해외 잔고 조회",
                    "/uapi/overseas-stock/v1/trading/inquire-balance", "TTTS3012R", page == 0 ? "" : "N",
                    params, KisDtos.OverseasBalance.class);
            for (KisDtos.OverseasItem item : nullSafe(res.body().output1())) {
                BigDecimal qty = number(item.quantity());
                if (qty.signum() <= 0) {
                    continue;
                }
                if (!"USD".equals(item.currency())) {
                    log.warn("지원하지 않는 통화라 동기화에서 제외: symbol={}, currency={}", item.symbol(), item.currency());
                    continue;
                }
                result.add(new SyncedHolding(AssetType.STOCK, item.name(), item.symbol(), account.label(),
                        qty, number(item.avgPrice()), number(item.lastPrice()), Currency.USD));
            }
            if (!res.hasNext()) {
                break;
            }
            fk = blankToEmpty(res.body().ctxFk());
            nk = blankToEmpty(res.body().ctxNk());
        }
        return result;
    }

    private static <T> List<T> nullSafe(List<T> list) {
        return list == null ? List.of() : list;
    }

    private static BigDecimal number(String s) {
        return s == null || s.isBlank() ? BigDecimal.ZERO : new BigDecimal(s.trim());
    }

    private static String blankToEmpty(String s) {
        return s == null ? "" : s.trim();
    }
}
