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
    void 가져오기는_한꺼번에_저장하고_하나라도_잘못되면_아무것도_저장하지_않는다() throws Exception {
        String before = mvc.perform(get("/api/study-cards")).andReturn().getResponse().getContentAsString();
        int countBefore = JsonPath.<java.util.List<Object>>read(before, "$").size();

        mvc.perform(post("/api/study-cards/import").contentType(MediaType.APPLICATION_JSON).content("""
                        {"cards":[
                          {"category":"INTRO","question":"가져온 질문1","answer":"답1","sortOrder":10},
                          {"category":"TECH","question":"가져온 질문2","answer":"답2"}
                        ]}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imported").value(2));

        // 두 번째 카드의 답이 비어 있으면 400, 첫 번째 카드도 저장되지 않는다
        mvc.perform(post("/api/study-cards/import").contentType(MediaType.APPLICATION_JSON).content("""
                        {"cards":[
                          {"category":"INTRO","question":"저장되면 안 됨","answer":"답"},
                          {"category":"TECH","question":"답 없음","answer":""}
                        ]}"""))
                .andExpect(status().isBadRequest());

        String after = mvc.perform(get("/api/study-cards")).andReturn().getResponse().getContentAsString();
        java.util.List<Integer> ids = JsonPath.read(after, "$[?(@.question =~ /가져온 질문.*/)].id");
        java.util.List<Object> blocked = JsonPath.read(after, "$[?(@.question == '저장되면 안 됨')]");
        org.assertj.core.api.Assertions.assertThat(JsonPath.<java.util.List<Object>>read(after, "$")).hasSize(countBefore + 2);
        org.assertj.core.api.Assertions.assertThat(blocked).isEmpty();
        for (Integer id : ids) mvc.perform(delete("/api/study-cards/" + id));
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
