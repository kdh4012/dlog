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
    void 가계부는_비로그인시_401() throws Exception {
        mvc.perform(get("/api/ledger")).andExpect(status().isUnauthorized());
    }

    @Test
    void 일기는_비로그인시_조회도_401() throws Exception {
        mvc.perform(get("/api/diary")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/goals")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/resolution")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/diary/1")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/diary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"entryDate":"2026-11-21","content":"c"}
                                """))
                .andExpect(status().isUnauthorized());
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
}
