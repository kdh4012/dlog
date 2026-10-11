package com.dlog.asset.sync.toss;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 토스증권 Open API 클라이언트. WTS > 설정 > Open API 에서 발급하고,
 * 같은 화면의 '허용 IP 관리'에 호출 서버의 공인 IP 를 등록해야 한다 (미등록 시 403).
 */
@ConfigurationProperties("dlog.toss")
public record TossProperties(
        @DefaultValue("https://openapi.tossinvest.com") String baseUrl,
        String clientId,
        String clientSecret
) {
    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
    }
}
