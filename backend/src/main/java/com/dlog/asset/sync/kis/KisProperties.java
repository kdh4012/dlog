package com.dlog.asset.sync.kis;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.Arrays;
import java.util.List;

/**
 * 한국투자증권 Open API (KIS Developers 에서 앱키 발급).
 *
 * @param accounts 조회할 계좌 목록. "계좌번호8자리-상품코드2자리" 를 쉼표로 구분 (예: 12345678-01,12345678-22)
 */
@ConfigurationProperties("dlog.kis")
public record KisProperties(
        @DefaultValue("https://openapi.koreainvestment.com:9443") String baseUrl,
        String appKey,
        String appSecret,
        String accounts
) {
    public boolean isConfigured() {
        return notBlank(appKey) && notBlank(appSecret) && !accountList().isEmpty();
    }

    /** 공백·하이픈 유무와 관계없이 8자리 + 2자리로 나눈다. 형식이 틀린 항목은 버린다 */
    public List<Account> accountList() {
        if (accounts == null) {
            return List.of();
        }
        return Arrays.stream(accounts.split(","))
                .map(s -> s.replaceAll("[^0-9]", ""))
                .filter(s -> s.length() == 10)
                .map(s -> new Account(s.substring(0, 8), s.substring(8)))
                .distinct()
                .toList();
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    /** @param cano 종합계좌번호 8자리, @param productCode 계좌상품코드 2자리 (01 종합, 22 연금저축 등) */
    public record Account(String cano, String productCode) {

        /** 연금저축(22)·IRP(29) 계좌는 해외주식을 직접 보유할 수 없어 해외 잔고 조회를 건너뛴다 */
        public boolean canHoldOverseas() {
            return !productCode.equals("22") && !productCode.equals("29");
        }

        /** 화면 표시용: 끝 4자리만 (예: 한국투자 ··9939-01) */
        public String label() {
            return "한국투자 ··" + cano.substring(4) + "-" + productCode;
        }
    }
}
