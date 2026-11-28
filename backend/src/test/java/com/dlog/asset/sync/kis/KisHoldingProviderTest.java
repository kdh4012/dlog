package com.dlog.asset.sync.kis;

import com.dlog.asset.AssetSource;
import com.dlog.asset.Currency;
import com.dlog.asset.sync.ExternalApiException;
import com.dlog.asset.sync.FakeApiServer;
import com.dlog.asset.sync.FakeApiServer.Response;
import com.dlog.asset.sync.SyncedHolding;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KisHoldingProviderTest {

    private final FakeApiServer server = new FakeApiServer();
    /** 잔고 조회 요청의 tr_id 헤더 기록 */
    private final List<String> trIds = new CopyOnWriteArrayList<>();

    @AfterEach
    void tearDown() {
        server.close();
    }

    private KisHoldingProvider provider(String accounts) {
        KisProperties props = new KisProperties(server.baseUrl(), "app", "secret", accounts);
        return new KisHoldingProvider(props, new KisApiClient(props));
    }

    private void tokenRoute() {
        server.route("/oauth2/tokenP", ex -> Response.ok("""
                {"access_token":"t1","token_type":"Bearer","expires_in":86400}"""));
    }

    private void domesticRoute() {
        server.route("/uapi/domestic-stock/v1/trading/inquire-balance", ex -> {
            trIds.add(ex.getRequestHeaders().getFirst("tr_id"));
            assertThat(ex.getRequestHeaders().getFirst("Authorization")).isEqualTo("Bearer t1");
            return Response.ok("""
                    {"rt_cd":"0","msg_cd":"KIOK0510","msg1":"조회되었습니다","ctx_area_fk100":"","ctx_area_nk100":"",
                     "output1":[
                       {"pdno":"005930","prdt_name":"삼성전자","hldg_qty":"10","pchs_avg_pric":"70000.0000","prpr":"75000"},
                       {"pdno":"000660","prdt_name":"SK하이닉스","hldg_qty":"0","pchs_avg_pric":"0","prpr":"200000"}
                     ],"output2":[{}]}""");
        });
    }

    @Test
    void 국내와_미국_잔고를_합치고_0주는_뺀다() {
        tokenRoute();
        domesticRoute();
        server.route("/uapi/overseas-stock/v1/trading/inquire-balance", ex -> {
            trIds.add(ex.getRequestHeaders().getFirst("tr_id"));
            return Response.ok("""
                    {"rt_cd":"0","msg_cd":"KIOK0510","msg1":"ok","output1":[
                      {"ovrs_pdno":"SOXL","ovrs_item_name":"디렉시온 반도체 3X","ovrs_cblc_qty":"5",
                       "pchs_avg_pric":"30.5","now_pric2":"28.12","tr_crcy_cd":"USD"}
                    ],"output2":{}}""");
        });

        List<SyncedHolding> holdings = provider("46479939-01").fetchHoldings();

        assertThat(holdings).extracting(SyncedHolding::ticker).containsExactly("005930", "SOXL");
        assertThat(holdings.get(0).currency()).isEqualTo(Currency.KRW);
        assertThat(holdings.get(0).currentPrice()).isEqualByComparingTo("75000");
        assertThat(holdings.get(1).currency()).isEqualTo(Currency.USD);
        assertThat(holdings.get(1).account()).isEqualTo("한국투자 ··9939-01");
        assertThat(trIds).containsExactly("TTTC8434R", "TTTS3012R");
        assertThat(server.requests.get(1)).contains("CANO=46479939").contains("ACNT_PRDT_CD=01");
        // 토큰은 한 번만 발급
        assertThat(server.count("POST /oauth2/tokenP")).isEqualTo(1);
    }

    @Test
    void 연금저축_계좌는_해외_잔고를_조회하지_않는다() {
        tokenRoute();
        domesticRoute();

        assertThat(provider("43112584-22").fetchHoldings()).hasSize(1);
        assertThat(server.count("GET /uapi/overseas-stock")).isZero();
    }

    @Test
    void rt_cd_가_0이_아니면_메시지를_담아_실패() {
        tokenRoute();
        server.route("/uapi/domestic-stock/v1/trading/inquire-balance", ex -> Response.ok("""
                {"rt_cd":"1","msg_cd":"EGW00123","msg1":"기간이 만료된 token 입니다."}"""));

        assertThatThrownBy(() -> provider("46479939-01").fetchHoldings())
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("EGW00123")
                .hasMessageContaining("만료");
    }

    @Test
    void 계좌_형식이_틀리면_미설정() {
        assertThat(provider("1234").isConfigured()).isFalse();
        assertThat(provider("46479939 01, 4311258422").isConfigured()).isTrue();
        assertThat(provider("").source()).isEqualTo(AssetSource.KIS);
    }
}
