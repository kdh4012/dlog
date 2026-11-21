package com.dlog.goal;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
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

@SpringBootTest(properties = "dlog.admin.auto-login=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GoalApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    GoalRepository repository;

    @AfterEach
    void cleanUp() {
        repository.deleteAll();
    }

    private Integer add(String json) throws Exception {
        String body = mvc.perform(post("/api/goals").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void 연도별로_등록한_순서대로_조회() throws Exception {
        int thisYear = GoalService.thisYear();
        add("{\"title\":\"정보처리기사 취득\"}"); // 연도 생략 → 올해
        add("{\"year\":%d,\"title\":\"  운동 주 3회  \"}".formatted(thisYear));
        add("{\"year\":2025,\"title\":\"작년 목표\"}");

        mvc.perform(get("/api/goals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("정보처리기사 취득"))
                .andExpect(jsonPath("$[0].year").value(thisYear))
                .andExpect(jsonPath("$[1].title").value("운동 주 3회"));
        mvc.perform(get("/api/goals").param("year", "2025"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("작년 목표"));
    }

    @Test
    void 제목_수정과_삭제() throws Exception {
        Integer id = add("{\"year\":2026,\"title\":\"초안\"}");

        mvc.perform(put("/api/goals/" + id).contentType(MediaType.APPLICATION_JSON).content("{\"year\":2020,\"title\":\"고친 목표\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("고친 목표"))
                .andExpect(jsonPath("$.year").value(2026)); // 연도는 바뀌지 않는다

        // 달성 표시 (기본은 미달성)
        mvc.perform(get("/api/goals").param("year", "2026")).andExpect(jsonPath("$[0].done").value(false));
        mvc.perform(patch("/api/goals/" + id + "/done").contentType(MediaType.APPLICATION_JSON).content("{\"done\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.done").value(true))
                .andExpect(jsonPath("$.title").value("고친 목표"));
        mvc.perform(patch("/api/goals/" + id + "/done").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        mvc.perform(delete("/api/goals/" + id)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/goals/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void 빈_제목은_400() throws Exception {
        mvc.perform(post("/api/goals").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 순서_변경() throws Exception {
        Integer a = add("{\"year\":2026,\"title\":\"A\"}");
        Integer b = add("{\"year\":2026,\"title\":\"B\"}");
        Integer c = add("{\"year\":2026,\"title\":\"C\"}");

        mvc.perform(put("/api/goals/order").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"year\":2026,\"ids\":[%d,%d,%d]}".formatted(c, a, b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("C"));

        // 새 목표는 맨 아래
        add("{\"year\":2026,\"title\":\"D\"}");
        mvc.perform(get("/api/goals").param("year", "2026"))
                .andExpect(jsonPath("$[0].title").value("C"))
                .andExpect(jsonPath("$[1].title").value("A"))
                .andExpect(jsonPath("$[2].title").value("B"))
                .andExpect(jsonPath("$[3].title").value("D"));

        // 일부만 보내면 400
        mvc.perform(put("/api/goals/order").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"year\":2026,\"ids\":[%d,%d]}".formatted(c, a)))
                .andExpect(status().isBadRequest());
    }
}
