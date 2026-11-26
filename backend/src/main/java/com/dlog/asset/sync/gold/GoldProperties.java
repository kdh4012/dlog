package com.dlog.asset.sync.gold;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * KRX 금 시세 (공공데이터포털 '금융위원회_일반상품시세정보'). 활용신청 후 받은 일반 인증키(Decoding)를 넣는다.
 * 키가 없으면 마지막 시세(없으면 매입 단가)로 평가한다.
 */
@ConfigurationProperties("dlog.gold")
public record GoldProperties(
        @DefaultValue("https://apis.data.go.kr/1160100/service/GetGeneralProductInfoService") String baseUrl,
        String apiKey
) {
    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
