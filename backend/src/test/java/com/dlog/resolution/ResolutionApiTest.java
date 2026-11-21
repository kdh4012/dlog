package com.dlog.resolution;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "dlog.admin.auto-login=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResolutionApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ResolutionRepository repository;

    @AfterEach
    void cleanUp() {
        repository.deleteAll();
    }

    private void save(String json) throws Exception {
        mvc.perform(put("/api/resolution").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk());
    }

    @Test
    void 처음엔_비어_있고_저장하면_덮어쓴다() throws Exception {
        mvc.perform(get("/api/resolution"))
                .andExpect(jsonPath("$.content").value(""))
                .andExpect(jsonPath("$.updatedAt").value(nullValue()));

        save("{\"content\":\"  흔들려도 매일 조금씩.\\n포기하지 않는다.  \"}");
        save("{\"content\":\"꾸준함이 재능을 이긴다.\"}");

        mvc.perform(get("/api/resolution"))
                .andExpect(jsonPath("$.content").value("꾸준함이 재능을 이긴다."))
                .andExpect(jsonPath("$.updatedAt").value(notNullValue()));
    }

    @Test
    void 빈_내용으로_저장하면_지운다() throws Exception {
        save("{\"content\":\"다짐\"}");
        save("{\"content\":\"   \"}");

        mvc.perform(get("/api/resolution")).andExpect(jsonPath("$.content").value(""));
    }

    @Test
    void 너무_길거나_내용이_없으면_400() throws Exception {
        mvc.perform(put("/api/resolution").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/resolution").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"" + "가".repeat(1001) + "\"}"))
                .andExpect(status().isBadRequest());
    }
}
