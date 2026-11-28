package com.dlog.asset.sync.gold;

import com.dlog.asset.sync.ExternalApiClients;
import com.dlog.asset.sync.ExternalApiException;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 공공데이터포털 '금융위원회_일반상품시세정보' 의 KRX 금시장 일별 시세.
 * 하루 한 번(전 영업일 기준) 갱신되는 데이터라 6시간 캐시한다.
 */
@Slf4j
@Component
public class GoldPriceClient {

    private static final Duration CACHE_TTL = Duration.ofHours(6);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter YMD = DateTimeFormatter.BASIC_ISO_DATE;
    /** 대표 종목: '금 99.99_1Kg' (가격은 g 당) */
    private static final String MAIN_ITEM = "1Kg";

    private final GoldProperties properties;
    private final RestClient client;

    private GoldPrice cached;
    private Instant cachedAt = Instant.EPOCH;

    public GoldPriceClient(GoldProperties properties) {
        this.properties = properties;
        this.client = ExternalApiClients.create(properties.baseUrl());
    }

    public boolean isConfigured() {
        return properties.hasApiKey();
    }

    /**
     * 가장 최근 영업일의 금 1g 종가. 키가 없으면 빈 값.
     *
     * @throws ExternalApiException 호출 실패·응답 오류
     */
    public synchronized Optional<GoldPrice> latest() {
        if (!properties.hasApiKey()) {
            return Optional.empty();
        }
        if (cached != null && Instant.now().isBefore(cachedAt.plus(CACHE_TTL))) {
            return Optional.of(cached);
        }
        cached = fetch();
        cachedAt = Instant.now();
        return Optional.of(cached);
    }

    private GoldPrice fetch() {
        // 연휴가 있어도 최근 영업일이 들어오도록 2주치를 받아 가장 최근 날짜를 고른다
        String begin = LocalDate.now(KST).minusDays(14).format(YMD);
        Envelope body;
        try {
            body = client.get()
                    .uri(b -> b.path("/getGoldPriceInfo_V2")
                            .queryParam("serviceKey", "{key}")
                            .queryParam("resultType", "json")
                            .queryParam("numOfRows", 50)
                            .queryParam("pageNo", 1)
                            .queryParam("beginBasDt", begin)
                            .build(properties.apiKey())) // 키의 +, /, = 를 인코딩해서 보낸다
                    .retrieve()
                    .body(Envelope.class);
        } catch (RestClientException e) {
            throw ExternalApiException.of("금 시세 조회", e);
        }
        if (body == null || body.response() == null || body.response().header() == null) {
            throw new ExternalApiException("금 시세 응답 형식이 올바르지 않습니다.");
        }
        Header header = body.response().header();
        if (!"00".equals(header.resultCode())) {
            throw new ExternalApiException("금 시세 조회 실패: " + header.resultCode() + " " + header.resultMsg());
        }
        List<Item> items = Optional.ofNullable(body.response().body())
                .map(Body::items).map(Items::item).orElse(List.of());
        // 대표 종목(1Kg)이 있으면 그것만, 없으면 전체 중 최신
        List<Item> main = items.stream().filter(i -> i.itmsNm() != null && i.itmsNm().contains(MAIN_ITEM)).toList();
        Item latest = (main.isEmpty() ? items : main).stream()
                .filter(i -> i.basDt() != null && i.clpr() != null)
                .max(Comparator.comparing(Item::basDt))
                .orElseThrow(() -> new ExternalApiException("최근 2주 금 시세가 없습니다."));
        GoldPrice price = new GoldPrice(new BigDecimal(latest.clpr().trim()), LocalDate.parse(latest.basDt(), YMD));
        log.info("금 시세 갱신: {}원/g ({})", price.pricePerGram(), price.date());
        return price;
    }

    // 응답: {"response":{"header":{...},"body":{"items":{"item":[{basDt, itmsNm, clpr, ...}]}}}}
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Envelope(Response response) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Response(Header header, Body body) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Header(String resultCode, String resultMsg) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Body(Items items) {
    }

    /** 결과가 한 건이면 배열이 아니라 객체로 오므로 둘 다 받는다 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Items(@JsonProperty("item") @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY) List<Item> item) {
    }

    /** clpr: 종가(원/g), basDt: 기준일자 yyyyMMdd */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Item(String basDt, String itmsNm, String clpr) {
    }
}
