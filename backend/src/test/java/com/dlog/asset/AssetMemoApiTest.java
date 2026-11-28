package com.dlog.asset;

import com.dlog.asset.memo.AssetMemoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "dlog.admin.auto-login=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AssetMemoApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    AssetMemoRepository repository;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    void 메모를_저장하고_수정하고_비우면_지운다() throws Exception {
        mvc.perform(get("/api/assets/memo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(""));

        mvc.perform(put("/api/assets/memo").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"  SOXL 1억 되면 절반 팔아 채권\\n금 추가  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("SOXL 1억 되면 절반 팔아 채권\n금 추가"));

        mvc.perform(put("/api/assets/memo").contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"수정\"}"))
                .andExpect(jsonPath("$.content").value("수정"));
        mvc.perform(get("/api/assets/memo")).andExpect(jsonPath("$.content").value("수정"));

        mvc.perform(put("/api/assets/memo").contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"  \"}"))
                .andExpect(jsonPath("$.content").value(""))
                .andExpect(jsonPath("$.updatedAt").doesNotExist());
    }
}
