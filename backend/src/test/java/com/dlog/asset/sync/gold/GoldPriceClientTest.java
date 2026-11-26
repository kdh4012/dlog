package com.dlog.asset.sync.gold;

import com.dlog.asset.sync.ExternalApiException;
import com.dlog.asset.sync.FakeApiServer;
import com.dlog.asset.sync.FakeApiServer.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GoldPriceClientTest {

    private final FakeApiServer server = new FakeApiServer();

    @AfterEach
    void tearDown() {
        server.close();
    }

    private GoldPriceClient client(String key) {
        return new GoldPriceClient(new GoldProperties(server.baseUrl(), key));
    }

    @Test
    void 대표_종목_1Kg_의_가장_최근_종가를_6시간_캐시() {
        server.route("/getGoldPriceInfo", ex -> Response.ok("""
                {"response":{"header":{"resultCode":"00","resultMsg":"NORMAL SERVICE."},
                 "body":{"numOfRows":50,"pageNo":1,"totalCount":4,"items":{"item":[
                   {"basDt":"20261124","itmsNm":"금 99.99_1Kg","clpr":"186000","srtnCd":"04020000"},
                   {"basDt":"20261125","itmsNm":"미니금 99.99_100g","clpr":"188500"},
                   {"basDt":"20261125","itmsNm":"금 99.99_1Kg","clpr":"187250"},
                   {"basDt":"20261121","itmsNm":"금 99.99_1Kg","clpr":"185100"}
                 ]}}}}"""));
        GoldPriceClient client = client("a+b/c=");

        GoldPrice price = client.latest().orElseThrow();
        client.latest();

        assertThat(price.pricePerGram()).isEqualByComparingTo("187250");
        assertThat(price.date()).isEqualTo(LocalDate.of(2026, 11, 25));
        assertThat(server.count("GET /getGoldPriceInfo")).isEqualTo(1);
        // 키의 +, /, = 는 인코딩되어 전달된다
        assertThat(server.requests.get(0)).contains("serviceKey=a%2Bb%2Fc%3D").contains("resultType=json");
    }

    @Test
    void 포털의_Encoding_키를_넣어도_이중_인코딩하지_않는다() {
        server.route("/getGoldPriceInfo", ex -> Response.ok("""
                {"response":{"header":{"resultCode":"00","resultMsg":"OK"},
                 "body":{"items":{"item":{"basDt":"20261125","itmsNm":"금 99.99_1Kg","clpr":"187250"}}}}}"""));

        client(" a%2Bb%2Fc%3D ").latest();

        assertThat(server.requests.get(0)).contains("serviceKey=a%2Bb%2Fc%3D").doesNotContain("%252");
    }

    @Test
    void 결과가_한_건이면_객체로_와도_읽는다() {
        server.route("/getGoldPriceInfo", ex -> Response.ok("""
                {"response":{"header":{"resultCode":"00","resultMsg":"OK"},
                 "body":{"items":{"item":{"basDt":"20261125","itmsNm":"금 99.99_1Kg","clpr":"187250"}}}}}"""));

        assertThat(client("k").latest().orElseThrow().pricePerGram()).isEqualByComparingTo("187250");
    }

    @Test
    void 키가_없으면_호출하지_않고_빈_값() {
        assertThat(client("").latest()).isEmpty();
        assertThat(server.requests).isEmpty();
    }

    @Test
    void 인증키_오류는_원인을_담은_예외() {
        server.route("/getGoldPriceInfo", ex -> Response.ok("""
                {"response":{"header":{"resultCode":"30","resultMsg":"SERVICE_KEY_IS_NOT_REGISTERED_ERROR"}}}"""));

        assertThatThrownBy(() -> client("bad").latest())
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("SERVICE_KEY_IS_NOT_REGISTERED_ERROR");
    }
}
