package com.dlog.asset.history;

import com.dlog.asset.AssetRepository;
import com.dlog.asset.sync.gold.GoldHoldingRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 금 보유량 저장 → 금 동기화 → 오늘의 총 자산 기록 (테스트에는 시세 키·환율이 없어 매입 단가·원화로 계산) */
@SpringBootTest(properties = "dlog.admin.auto-login=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AssetHistoryApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    AssetSnapshotRepository snapshotRepository;

    @Autowired
    GoldHoldingRepository goldRepository;

    @Autowired
    AssetRepository assetRepository;

    /** 다른 테스트가 남긴 자산·기록이 섞이지 않도록 앞뒤로 비운다 */
    @BeforeEach
    @AfterEach
    void cleanUp() {
        snapshotRepository.deleteAll();
        goldRepository.deleteAll();
        assetRepository.deleteAll();
    }

    private void saveGold(String json) throws Exception {
        mvc.perform(put("/api/assets/gold").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk());
    }

    @Test
    void 동기화하면_오늘_총자산을_기록하고_같은_날은_덮어쓴다() throws Exception {
        String today = LocalDate.now(ZoneId.of("Asia/Seoul")).toString();

        mvc.perform(get("/api/assets/history")).andExpect(jsonPath("$.length()").value(0));

        saveGold("{\"grams\":10,\"avgPrice\":150000}");
        mvc.perform(get("/api/assets/history"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].date").value(today))
                .andExpect(jsonPath("$[0].evalKrw").value(1500000.0))
                .andExpect(jsonPath("$[0].costKrw").value(1500000.0));

        saveGold("{\"grams\":20,\"avgPrice\":150000}");
        mvc.perform(get("/api/assets/history"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].evalKrw").value(3000000.0));
    }

    @Test
    void 자산이_없으면_기록하지_않는다() throws Exception {
        mvc.perform(post("/api/assets/sync"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/assets/history")).andExpect(jsonPath("$.length()").value(0));
    }
}
