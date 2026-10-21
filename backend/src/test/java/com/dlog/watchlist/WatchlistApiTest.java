package com.dlog.watchlist;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 테스트 프로파일은 시세 API 가 연결 거부라 시세 없이 목록만 확인한다 (시세 파싱은 StockQuoteServiceTest) */
@SpringBootTest(properties = "dlog.admin.auto-login=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WatchlistApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    WatchlistRepository repository;

    @AfterEach
    void cleanUp() {
        repository.deleteAll();
    }

    private String add(String json) throws Exception {
        return mvc.perform(post("/api/watchlist").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void 추가_조회_수정_삭제() throws Exception {
        String body = add("""
                {"ticker":" nvda ","name":"엔비디아","targetPrice":150.5,"memo":"AI 반도체"}""");
        Integer id = JsonPath.read(body, "$.id");
        add("""
                {"ticker":"brk-b"}""");

        mvc.perform(get("/api/watchlist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].ticker").value("NVDA"))
                .andExpect(jsonPath("$[0].name").value("엔비디아"))
                .andExpect(jsonPath("$[0].targetPrice").value(150.5))
                .andExpect(jsonPath("$[0].price").value(nullValue())) // 시세 없음
                .andExpect(jsonPath("$[1].ticker").value("BRK-B"))
                .andExpect(jsonPath("$[1].name").value(nullValue()));

        mvc.perform(put("/api/watchlist/" + id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"ticker":"IGNORED","name":"NVIDIA","targetPrice":null,"memo":""}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticker").value("NVDA"))
                .andExpect(jsonPath("$.name").value("NVIDIA"))
                .andExpect(jsonPath("$.targetPrice").value(nullValue()))
                .andExpect(jsonPath("$.memo").value(nullValue()));

        mvc.perform(delete("/api/watchlist/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/watchlist")).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void 같은_티커는_중복_추가_불가() throws Exception {
        add("""
                {"ticker":"AAPL"}""");
        mvc.perform(post("/api/watchlist").contentType(MediaType.APPLICATION_JSON).content("""
                        {"ticker":"aapl"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WATCHLIST_DUPLICATE"));
    }

    @Test
    void 잘못된_입력은_400() throws Exception {
        mvc.perform(post("/api/watchlist").contentType(MediaType.APPLICATION_JSON).content("""
                        {"ticker":"애플"}"""))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/watchlist").contentType(MediaType.APPLICATION_JSON).content("""
                        {"ticker":"AAPL","targetPrice":-1}"""))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/watchlist/9999")).andExpect(status().isNotFound());
    }
}
