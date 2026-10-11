package com.dlog.expense;

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

@SpringBootTest(properties = "dlog.admin.auto-login=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExpenseApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ExpenseRepository expenseRepository;

    @Autowired
    ExpenseBudgetRepository budgetRepository;

    @AfterEach
    void cleanUp() {
        expenseRepository.deleteAll();
        budgetRepository.deleteAll();
    }

    private void add(String date, long amount, String category) throws Exception {
        mvc.perform(post("/api/expenses").contentType(MediaType.APPLICATION_JSON).content("""
                        {"spentOn":"%s","amount":%d,"category":"%s","paymentMethod":"CARD","merchant":" ","memo":"m"}"""
                        .formatted(date, amount, category)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.merchant").value(nullValue()));
    }

    private void budget(String month, long amount) throws Exception {
        mvc.perform(put("/api/expenses/budget").contentType(MediaType.APPLICATION_JSON).content("""
                        {"month":"%s","amount":%d}""".formatted(month, amount)))
                .andExpect(status().isNoContent());
    }

    @Test
    void 월_요약은_카테고리별_합계와_전월_대비를_계산한다() throws Exception {
        add("2026-09-30", 100_000, "FOOD");
        add("2026-10-01", 30_000, "FOOD");
        add("2026-10-15", 50_000, "SHOPPING");
        add("2026-10-31", 40_000, "FOOD");
        add("2026-11-01", 999_999, "TRAVEL");

        mvc.perform(get("/api/expenses").param("month", "2026-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].spentOn").value("2026-10-31"));

        mvc.perform(get("/api/expenses/summary").param("month", "2026-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("2026-10"))
                .andExpect(jsonPath("$.total").value(120_000))
                .andExpect(jsonPath("$.count").value(3))
                .andExpect(jsonPath("$.previousTotal").value(100_000))
                .andExpect(jsonPath("$.changeRate").value(20.00))
                .andExpect(jsonPath("$.byCategory[0].category").value("FOOD"))
                .andExpect(jsonPath("$.byCategory[0].amount").value(70_000))
                .andExpect(jsonPath("$.byCategory[0].count").value(2))
                .andExpect(jsonPath("$.byCategory[0].weight").value(58.33))
                .andExpect(jsonPath("$.trend.length()").value(6))
                .andExpect(jsonPath("$.trend[4].month").value("2026-09"))
                .andExpect(jsonPath("$.trend[4].total").value(100_000))
                .andExpect(jsonPath("$.trend[5].total").value(120_000))
                .andExpect(jsonPath("$.budget").value(nullValue()))
                .andExpect(jsonPath("$.budgetUsedRate").value(nullValue()));
    }

    @Test
    void 예산은_이후_달로_이어지고_0이면_해당_월만_지운다() throws Exception {
        add("2026-10-05", 300_000, "FOOD");
        budget("2026-08", 1_000_000);

        // 9, 10월은 8월 예산을 이어받는다
        mvc.perform(get("/api/expenses/summary").param("month", "2026-10"))
                .andExpect(jsonPath("$.budget").value(1_000_000))
                .andExpect(jsonPath("$.budgetMonth").value("2026-08"))
                .andExpect(jsonPath("$.budgetUsedRate").value(30.00));
        // 이전 달에는 적용되지 않는다
        mvc.perform(get("/api/expenses/summary").param("month", "2026-07"))
                .andExpect(jsonPath("$.budget").value(nullValue()));

        budget("2026-10", 500_000);
        mvc.perform(get("/api/expenses/summary").param("month", "2026-10"))
                .andExpect(jsonPath("$.budget").value(500_000))
                .andExpect(jsonPath("$.budgetUsedRate").value(60.00));

        budget("2026-10", 0);
        mvc.perform(get("/api/expenses/summary").param("month", "2026-10"))
                .andExpect(jsonPath("$.budget").value(1_000_000));
    }

    @Test
    void 수정과_삭제() throws Exception {
        add("2026-10-05", 10_000, "CAFE");
        Long id = expenseRepository.findAll().get(0).getId();

        mvc.perform(put("/api/expenses/" + id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"spentOn":"2026-10-06","amount":12000,"category":"FOOD","paymentMethod":"CASH","merchant":"김밥집"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(12000))
                .andExpect(jsonPath("$.paymentMethod").value("CASH"))
                .andExpect(jsonPath("$.merchant").value("김밥집"));

        mvc.perform(delete("/api/expenses/" + id)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/expenses/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EXPENSE_NOT_FOUND"));
    }

    @Test
    void 잘못된_입력은_400() throws Exception {
        mvc.perform(post("/api/expenses").contentType(MediaType.APPLICATION_JSON).content("""
                        {"spentOn":"2026-10-05","amount":0,"category":"FOOD","paymentMethod":"CARD"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        mvc.perform(get("/api/expenses/summary").param("month", "2026-13"))
                .andExpect(status().isBadRequest());
    }
}
