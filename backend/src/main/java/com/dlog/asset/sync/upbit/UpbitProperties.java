package com.dlog.asset.sync.upbit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 업비트 Open API 키. 업비트 > 마이페이지 > Open API 관리에서 '자산조회' 권한만 부여해 발급하고,
 * 호출 서버의 공인 IP 를 허용 IP 로 등록해야 한다.
 */
@ConfigurationProperties("dlog.upbit")
public record UpbitProperties(
        @DefaultValue("https://api.upbit.com") String baseUrl,
        String accessKey,
        String secretKey
) {
    public boolean isConfigured() {
        return accessKey != null && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank();
    }
}
