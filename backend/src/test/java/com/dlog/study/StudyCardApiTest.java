package com.dlog.study;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 다른 카드가 함께 들어 있을 수 있으므로, 테스트는 자기가 만든 카드 id 로만 검증한다 */
@SpringBootTest(properties = "dlog.admin.auto-login=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StudyCardApiTest {

    @Autowired
    MockMvc mvc;

    private Integer create(String category, String question, int sortOrder) throws Exception {
        String body = mvc.perform(post("/api/study-cards").contentType(MediaType.APPLICATION_JSON).content("""
                        {"category":"%s","question":"%s","answer":"**답**","sortOrder":%d}"""
                        .formatted(category, question, sortOrder)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memorized").value(false))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void 생성_수정_외움표시_삭제() throws Exception {
        Integer id = create("TECH", "낙관적 락이란?", 5);

        mvc.perform(put("/api/study-cards/" + id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"category":"INTERVIEW","question":"수정된 질문","answer":"수정된 답","sortOrder":1}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("INTERVIEW"))
                .andExpect(jsonPath("$.question").value("수정된 질문"))
                .andExpect(jsonPath("$.sortOrder").value(1));

        mvc.perform(patch("/api/study-cards/" + id + "/memorized").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memorized\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memorized").value(true));

        mvc.perform(get("/api/study-cards/" + id))
                .andExpect(jsonPath("$.memorized").value(true))
                .andExpect(jsonPath("$.answer").value("수정된 답"));

        mvc.perform(delete("/api/study-cards/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/study-cards/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void 분류별_조회는_순서대로() throws Exception {
        Integer second = create("DOMAIN", "두번째", 20);
        Integer first = create("DOMAIN", "첫번째", 10);

        String body = mvc.perform(get("/api/study-cards").param("category", "DOMAIN"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        java.util.List<Integer> ids = JsonPath.read(body, "$[*].id");
        java.util.List<String> categories = JsonPath.read(body, "$[*].category");

        org.assertj.core.api.Assertions.assertThat(categories).containsOnly("DOMAIN");
        org.assertj.core.api.Assertions.assertThat(ids.indexOf(first)).isLessThan(ids.indexOf(second));

        mvc.perform(delete("/api/study-cards/" + first));
        mvc.perform(delete("/api/study-cards/" + second));
    }

    @Test
    void 필수값_누락과_잘못된_분류는_400() throws Exception {
        mvc.perform(post("/api/study-cards").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"TECH\",\"question\":\"\",\"answer\":\"a\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/study-cards").param("category", "NOPE"))
                .andExpect(status().isBadRequest());
    }
}
