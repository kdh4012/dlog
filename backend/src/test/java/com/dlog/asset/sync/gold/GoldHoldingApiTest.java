package com.dlog.asset.sync.gold;

import com.dlog.asset.AssetRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 테스트 프로파일에는 시세 API 키가 없으므로 매입 단가로 평가된다 */
@SpringBootTest(properties = "dlog.admin.auto-login=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GoldHoldingApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    GoldHoldingRepository repository;

    @Autowired
    AssetRepository assetRepository;

    @AfterEach
    void cleanUp() {
        repository.deleteAll();
        assetRepository.deleteAll();
    }

    private void save(String json) throws Exception {
        mvc.perform(put("/api/assets/gold").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk());
    }

    @Test
    void 보유량을_넣으면_금_자산이_생기고_0이면_지운다() throws Exception {
        mvc.perform(get("/api/assets/gold"))
                .andExpect(jsonPath("$.grams").value(0))
                .andExpect(jsonPath("$.priceApiReady").value(false));

        save("{\"grams\":12.5,\"avgPrice\":150000}");
        mvc.perform(get("/api/assets/gold"))
                .andExpect(jsonPath("$.grams").value(12.5))
                .andExpect(jsonPath("$.currentPrice").value(150000)) // 시세가 없으면 매입 단가
                .andExpect(jsonPath("$.priceDate").value(nullValue()));
        mvc.perform(get("/api/assets"))
                .andExpect(jsonPath("$[?(@.source == 'GOLD')].name").value("금현물 (KRX)"))
                .andExpect(jsonPath("$[?(@.source == 'GOLD')].evalAmount").value(1875000.0))
                .andExpect(jsonPath("$[?(@.source == 'GOLD')].currency").value("KRW"));

        // 고치면 같은 자산이 갱신된다
        save("{\"grams\":20,\"avgPrice\":160000}");
        mvc.perform(get("/api/assets"))
                .andExpect(jsonPath("$[?(@.source == 'GOLD')].quantity").value(20.0));

        save("{\"grams\":0,\"avgPrice\":0}");
        mvc.perform(get("/api/assets")).andExpect(jsonPath("$[?(@.source == 'GOLD')]").isEmpty());
        mvc.perform(get("/api/assets/sync")).andExpect(status().isOk());
    }

    @Test
    void 음수나_빈_값은_400() throws Exception {
        mvc.perform(put("/api/assets/gold").contentType(MediaType.APPLICATION_JSON).content("{\"grams\":-1,\"avgPrice\":100}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/assets/gold").contentType(MediaType.APPLICATION_JSON).content("{\"grams\":1}"))
                .andExpect(status().isBadRequest());
    }
}
