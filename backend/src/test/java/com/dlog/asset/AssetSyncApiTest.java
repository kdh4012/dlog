package com.dlog.asset;

import com.dlog.asset.sync.SyncedHolding;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 동기화 자산 upsert 와 읽기 전용 보호. 테스트 프로파일은 API 키가 없어 실제 외부 호출은 없다 */
@SpringBootTest(properties = "dlog.admin.auto-login=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AssetSyncApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    AssetService assetService;

    @Autowired
    AssetRepository assetRepository;

    @AfterEach
    void cleanUp() {
        assetRepository.deleteAll();
    }

    private static SyncedHolding btc(String quantity) {
        return new SyncedHolding(AssetType.CRYPTO, "비트코인", "BTC", "업비트",
                new BigDecimal(quantity), new BigDecimal("90000000"), new BigDecimal("95000000"), Currency.KRW);
    }

    private static SyncedHolding eth() {
        return new SyncedHolding(AssetType.CRYPTO, "이더리움", "ETH", "업비트",
                BigDecimal.ONE, new BigDecimal("4000000"), new BigDecimal("4100000"), Currency.KRW);
    }

    @Test
    void 동기화는_갱신_추가_삭제를_반영하고_수동자산은_건드리지_않는다() throws Exception {
        mvc.perform(post("/api/assets").contentType(MediaType.APPLICATION_JSON).content("""
                        {"assetType":"DEPOSIT","name":"정기예금","quantity":1,"avgPrice":1000000,"currentPrice":1000000,"currency":"KRW"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("MANUAL"));

        assetService.replaceSynced(AssetSource.UPBIT, List.of(btc("0.01"), eth()));
        Long btcId = assetRepository.findAllBySource(AssetSource.UPBIT).stream()
                .filter(a -> a.getTicker().equals("BTC")).findFirst().orElseThrow().getId();

        // BTC 수량 변경, ETH 전량 매도
        assetService.replaceSynced(AssetSource.UPBIT, List.of(btc("0.02")));

        List<Asset> upbit = assetRepository.findAllBySource(AssetSource.UPBIT);
        assertThat(upbit).singleElement().satisfies(a -> {
            assertThat(a.getId()).isEqualTo(btcId); // 같은 행을 갱신
            assertThat(a.getQuantity()).isEqualByComparingTo("0.02");
        });
        assertThat(assetRepository.findAllBySource(AssetSource.MANUAL)).hasSize(1);
    }

    @Test
    void 동기화_자산은_수정_삭제_불가() throws Exception {
        assetService.replaceSynced(AssetSource.UPBIT, List.of(btc("0.01")));
        Long id = assetRepository.findAllBySource(AssetSource.UPBIT).get(0).getId();

        mvc.perform(put("/api/assets/" + id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"assetType":"CRYPTO","name":"x","quantity":1,"avgPrice":1,"currentPrice":1,"currency":"KRW"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ASSET_SYNCED_READONLY"));
        mvc.perform(delete("/api/assets/" + id))
                .andExpect(status().isConflict());
    }

    @Test
    void 키가_없으면_동기화하지_않고_미설정_상태를_반환() throws Exception {
        String body = mvc.perform(post("/api/assets/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andReturn().getResponse().getContentAsString();

        List<Boolean> configured = JsonPath.read(body, "$[*].configured");
        assertThat(configured).containsOnly(false);
        mvc.perform(get("/api/assets/sync")).andExpect(jsonPath("$[0].source").value("UPBIT"));
    }
}
