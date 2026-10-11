package com.dlog.expense;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.YearMonth;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 고정지출은 '이번 달'(한국 시간) 기준으로 반영되므로 테스트도 현재 월 기준 상대 날짜를 쓴다 */
@SpringBootTest(properties = "dlog.admin.auto-login=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FixedExpenseApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ExpenseRepository expenseRepository;

    @Autowired
    FixedExpenseRepository fixedExpenseRepository;

    final YearMonth now = FixedExpenseService.thisMonth();

    @AfterEach
    void cleanUp() {
        expenseRepository.deleteAll();
        fixedExpenseRepository.deleteAll();
    }

    /** 고정지출 등록 후 id 반환 */
    private Integer createFixed(String name, long amount, int day, YearMonth start, YearMonth end) throws Exception {
        String endJson = end == null ? "null" : "\"" + end + "\"";
        String body = mvc.perform(post("/api/fixed-expenses").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"%s","amount":%d,"dayOfMonth":%d,"category":"HOUSING","paymentMethod":"TRANSFER",
                         "startMonth":"%s","endMonth":%s}""".formatted(name, amount, day, start, endJson)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void 등록하면_시작월부터_이번달까지_매월_반영되고_미래달은_만들지_않는다() throws Exception {
        createFixed("월세", 500_000, 31, now.minusMonths(2), null);
        mvc.perform(post("/api/expenses").contentType(MediaType.APPLICATION_JSON).content("""
                        {"spentOn":"%s","amount":10000,"category":"FOOD","paymentMethod":"CARD"}""".formatted(now.atDay(1))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.kind").value("VARIABLE"));

        // 요약 조회 시 최근 6개월 범위의 고정지출이 반영된다
        mvc.perform(get("/api/expenses/summary").param("month", now.toString()))
                .andExpect(jsonPath("$.total").value(510_000))
                .andExpect(jsonPath("$.fixedTotal").value(500_000))
                .andExpect(jsonPath("$.variableTotal").value(10_000))
                .andExpect(jsonPath("$.trend[3].total").value(500_000)) // 2달 전
                .andExpect(jsonPath("$.trend[2].total").value(0));      // 시작 전 달

        // 31일 결제는 짧은 달이면 말일로 반영
        mvc.perform(get("/api/expenses").param("month", now.toString()))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].kind").value("FIXED"))
                .andExpect(jsonPath("$[0].merchant").value("월세"))
                .andExpect(jsonPath("$[0].spentOn").value(now.atEndOfMonth().toString()));

        mvc.perform(get("/api/expenses").param("month", now.plusMonths(1).toString()))
                .andExpect(jsonPath("$.length()").value(0));

        // 같은 달을 여러 번 조회해도 한 번만 반영
        mvc.perform(get("/api/expenses/summary").param("month", now.toString()))
                .andExpect(jsonPath("$.fixedTotal").value(500_000));
    }

    @Test
    void 반영된_내역은_그달만_수정되고_지우면_다시_생기지_않는다() throws Exception {
        createFixed("관리비", 200_000, 10, now, null);
        String list = mvc.perform(get("/api/expenses").param("month", now.toString()))
                .andReturn().getResponse().getContentAsString();
        Integer expenseId = JsonPath.read(list, "$[0].id");

        mvc.perform(put("/api/expenses/" + expenseId).contentType(MediaType.APPLICATION_JSON).content("""
                        {"spentOn":"%s","amount":215000,"category":"HOUSING","paymentMethod":"TRANSFER","merchant":"관리비"}"""
                        .formatted(now.atDay(10))))
                .andExpect(jsonPath("$.amount").value(215_000))
                .andExpect(jsonPath("$.kind").value("FIXED"));

        mvc.perform(delete("/api/expenses/" + expenseId)).andExpect(status().isNoContent());
        mvc.perform(get("/api/expenses").param("month", now.toString()))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void 종료월이_지나면_반영하지_않고_비활성으로_표시() throws Exception {
        createFixed("헬스장", 50_000, 1, now.minusMonths(3), now.minusMonths(1));

        mvc.perform(get("/api/expenses/summary").param("month", now.toString()))
                .andExpect(jsonPath("$.fixedTotal").value(0))
                .andExpect(jsonPath("$.trend[4].total").value(50_000)); // 지난달은 반영
        mvc.perform(get("/api/fixed-expenses"))
                .andExpect(jsonPath("$[0].active").value(false))
                .andExpect(jsonPath("$[0].endMonth").value(now.minusMonths(1).toString()));
    }

    @Test
    void 템플릿을_지워도_지난_내역은_고정지출로_남는다() throws Exception {
        Integer id = createFixed("보험", 80_000, 5, now, null);
        mvc.perform(get("/api/expenses").param("month", now.toString()))
                .andExpect(jsonPath("$[0].fixedExpenseId").value(id));

        mvc.perform(delete("/api/fixed-expenses/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/expenses").param("month", now.toString()))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].kind").value("FIXED"))
                .andExpect(jsonPath("$[0].fixedExpenseId").value(nullValue()));
    }

    @Test
    void 잘못된_입력은_400() throws Exception {
        mvc.perform(post("/api/fixed-expenses").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"x","amount":1000,"dayOfMonth":32,"category":"ETC","paymentMethod":"CARD","startMonth":"2026-10"}"""))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/fixed-expenses").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"x","amount":1000,"dayOfMonth":1,"category":"ETC","paymentMethod":"CARD",
                         "startMonth":"2026-10","endMonth":"2026-09"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        mvc.perform(get("/api/fixed-expenses")).andExpect(status().isOk());
    }
}
