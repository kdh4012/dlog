package com.dlog.asset.sync.kis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** 한국투자증권 잔고 조회 응답 (필요한 필드만). 숫자는 모두 문자열로 온다 */
final class KisDtos {

    private KisDtos() {
    }

    /** 국내주식 잔고조회 (TTTC8434R) */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record DomesticBalance(
            @JsonProperty("rt_cd") String rtCd,
            @JsonProperty("msg_cd") String msgCd,
            @JsonProperty("msg1") String msg1,
            @JsonProperty("ctx_area_fk100") String ctxFk,
            @JsonProperty("ctx_area_nk100") String ctxNk,
            List<DomesticItem> output1
    ) implements KisApiClient.Result {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record DomesticItem(
            /* 종목코드 */ String pdno,
            @JsonProperty("prdt_name") String name,
            @JsonProperty("hldg_qty") String quantity,
            @JsonProperty("pchs_avg_pric") String avgPrice,
            /* 현재가 */ String prpr
    ) {
    }

    /** 해외주식 잔고 (TTTS3012R) */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record OverseasBalance(
            @JsonProperty("rt_cd") String rtCd,
            @JsonProperty("msg_cd") String msgCd,
            @JsonProperty("msg1") String msg1,
            @JsonProperty("ctx_area_fk200") String ctxFk,
            @JsonProperty("ctx_area_nk200") String ctxNk,
            List<OverseasItem> output1
    ) implements KisApiClient.Result {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OverseasItem(
            @JsonProperty("ovrs_pdno") String symbol,
            @JsonProperty("ovrs_item_name") String name,
            @JsonProperty("ovrs_cblc_qty") String quantity,
            @JsonProperty("pchs_avg_pric") String avgPrice,
            @JsonProperty("now_pric2") String lastPrice,
            @JsonProperty("tr_crcy_cd") String currency
    ) {
    }
}
