package com.dlog.asset.sync.toss;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** 토스증권 Open API 응답 DTO (OpenAPI 1.2.24 기준). 금액·수량은 decimal 문자열이다 */
final class TossDtos {

    private TossDtos() {
    }

    /** POST /oauth2/token - OAuth2 표준 형식 (공통 envelope 아님) */
    record Token(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") long expiresIn
    ) {
    }

    /** GET /api/v1/accounts. accountSeq 는 계좌 API 의 X-Tossinvest-Account 헤더 값 */
    record Account(
            String accountNo,
            long accountSeq,
            String accountType
    ) {
    }

    /** GET /api/v1/holdings */
    record HoldingsOverview(List<HoldingsItem> items) {
    }

    /**
     * @param marketCountry KR / US (미지의 값 허용 필요)
     * @param currency      KRW / USD (미지의 값 허용 필요)
     * @param lastPrice     현재가 (거래 통화 기준)
     */
    record HoldingsItem(
            String symbol,
            String name,
            String marketCountry,
            String currency,
            String quantity,
            String lastPrice,
            String averagePurchasePrice
    ) {
    }
}
