package com.dlog;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DlogApiTest {

    @Autowired
    MockMvc mvc;

    private String login() throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"dlog1234"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.accessToken");
    }

    @Test
    void 잘못된_비밀번호는_401() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("LOGIN_FAILED"));
    }

    @Test
    void 자산은_비로그인시_401() throws Exception {
        mvc.perform(get("/api/assets")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/assets/summary")).andExpect(status().isUnauthorized());
    }

    @Test
    void 게시글_작성은_비로그인시_401() throws Exception {
        mvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"t","content":"c","published":true}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 비공개_글은_관리자에게만_보인다() throws Exception {
        String token = login();

        String body = mvc.perform(post("/api/posts")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"임시글","content":"본문","category":"dev","published":false}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(body, "$.id");

        mvc.perform(get("/api/posts/" + id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/posts/" + id).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("임시글"));
    }

    @Test
    void 자산_요약은_통화별로_수익률을_계산한다() throws Exception {
        String token = login();

        mvc.perform(post("/api/assets")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"assetType":"STOCK","name":"삼성전자","ticker":"005930","quantity":10,
                                 "avgPrice":70000,"currentPrice":77000,"currency":"KRW"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.profitAmount").value(70000.00))
                .andExpect(jsonPath("$.profitRate").value(10.00));

        mvc.perform(get("/api/assets/summary").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currencies[0].currency").value("KRW"))
                .andExpect(jsonPath("$.currencies[0].profitRate").value(10.00))
                .andExpect(jsonPath("$.currencies[0].byType[0].weight").value(100.00));
    }

    @Test
    void 작업기록_종료일이_시작일보다_빠르면_400() throws Exception {
        mvc.perform(post("/api/work-logs")
                        .header("Authorization", login())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"MES 구축","startDate":"2024-05-01","endDate":"2024-01-01"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }
}
