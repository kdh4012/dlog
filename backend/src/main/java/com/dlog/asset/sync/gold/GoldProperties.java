package com.dlog.asset.sync.gold;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * KRX 금 시세 (공공데이터포털 '금융위원회_일반상품시세정보'). 활용신청 후 받은 일반 인증키를 넣는다.
 * 포털에 보이는 Encoding 키(%2F, %2B, %3D 포함)든 Decoding 키든 그대로 넣으면 된다.
 * 키가 없으면 마지막 시세(없으면 매입 단가)로 평가한다.
 */
@ConfigurationProperties("dlog.gold")
public record GoldProperties(
        @DefaultValue("https://apis.data.go.kr/1160100/service/GetGeneralProductInfoService") String baseUrl,
        String apiKey
) {
    public GoldProperties {
        // 요청할 때 다시 인코딩하므로, Encoding 키가 들어오면 미리 풀어서 이중 인코딩을 막는다
        if (apiKey != null) {
            apiKey = apiKey.strip();
            if (apiKey.contains("%")) apiKey = URLDecoder.decode(apiKey, StandardCharsets.UTF_8);
        }
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
