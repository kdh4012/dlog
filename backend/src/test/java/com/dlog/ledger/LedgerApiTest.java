package com.dlog.ledger;

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
import java.util.List;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 이월은 '이번 달'(한국 시간)까지만 하므로 테스트도 현재 월 기준 상대 날짜를 쓴다 */
@SpringBootTest(properties = "dlog.admin.auto-login=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LedgerApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    LedgerRepository repository;

    @Autowired
    LedgerMonthRepository monthRepository;

    final YearMonth now = LedgerService.thisMonth();

    @AfterEach
    void cleanUp() {
        repository.deleteAll();
        monthRepository.deleteAll();
    }

    private Integer add(String kind, YearMonth month, String name, long amount) throws Exception {
        String monthJson = month == null ? "null" : "\"" + month + "\"";
        String body = mvc.perform(post("/api/ledger").contentType(MediaType.APPLICATION_JSON).content("""
                        {"kind":"%s","month":%s,"name":"%s","amount":%d}""".formatted(kind, monthJson, name, amount)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void 구분별_합계와_남는_돈_금액_큰_순() throws Exception {
        add("INCOME", now, "배당", 200_000);
        add("INCOME", now, "월급", 3_500_000);
        add("FIXED_EXPENSE", now, "통신비", 55_000);
        add("FIXED_EXPENSE", now, "월세", 600_000);
        add("DEBT", null, "학자금", 8_000_000);
        add("DEBT", null, "전세대출", 100_000_000);

        // month 생략 시 이번 달
        mvc.perform(get("/api/ledger"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value(now.toString()))
                .andExpect(jsonPath("$.incomeTotal").value(3_700_000))
                .andExpect(jsonPath("$.fixedExpenseTotal").value(655_000))
                .andExpect(jsonPath("$.monthlyBalance").value(3_045_000))
                .andExpect(jsonPath("$.debtTotal").value(108_000_000))
                .andExpect(jsonPath("$.incomes[0].name").value("월급"))
                .andExpect(jsonPath("$.incomes[0].month").value(now.toString()))
                .andExpect(jsonPath("$.fixedExpenses[0].name").value("월세"))
                .andExpect(jsonPath("$.debts[0].name").value("전세대출"))
                .andExpect(jsonPath("$.debts[0].month").value(nullValue()))
                .andExpect(jsonPath("$.trend.length()").value(12))
                .andExpect(jsonPath("$.trend[11].month").value(now.toString()))
                .andExpect(jsonPath("$.trend[11].income").value(3_700_000))
                .andExpect(jsonPath("$.trend[11].fixedExpense").value(655_000));
    }

    @Test
    void 빈_달은_가장_최근_달을_이월하고_그_달만_수정된다() throws Exception {
        YearMonth twoAgo = now.minusMonths(2);
        add("INCOME", twoAgo, "월급", 3_000_000);
        add("FIXED_EXPENSE", twoAgo, "월세", 600_000);
        add("DEBT", null, "전세대출", 100_000_000);

        // 이번 달을 열면 지난달까지 함께 채워진다
        String body = mvc.perform(get("/api/ledger").param("month", now.toString()))
                .andExpect(jsonPath("$.incomeTotal").value(3_000_000))
                .andExpect(jsonPath("$.fixedExpenseTotal").value(600_000))
                .andExpect(jsonPath("$.debtTotal").value(100_000_000))
                .andExpect(jsonPath("$.trend[9].income").value(3_000_000))  // 2달 전
                .andExpect(jsonPath("$.trend[10].income").value(3_000_000)) // 지난달 (이월)
                .andExpect(jsonPath("$.trend[8].income").value(0))          // 기록 전
                .andReturn().getResponse().getContentAsString();
        Integer salaryNow = JsonPath.read(body, "$.incomes[0].id");

        // 이번 달 월급만 올린다 (지난달은 그대로)
        mvc.perform(put("/api/ledger/" + salaryNow).contentType(MediaType.APPLICATION_JSON).content("""
                        {"kind":"INCOME","name":"월급","amount":3200000}"""))
                .andExpect(jsonPath("$.amount").value(3_200_000));
        mvc.perform(get("/api/ledger").param("month", now.minusMonths(1).toString()))
                .andExpect(jsonPath("$.incomeTotal").value(3_000_000));
        mvc.perform(get("/api/ledger").param("month", now.toString()))
                .andExpect(jsonPath("$.trend[11].income").value(3_200_000));

        // 이번 달 항목을 다 지워도 다시 이월하지 않는다
        String current = mvc.perform(get("/api/ledger")).andReturn().getResponse().getContentAsString();
        for (String path : new String[]{"$.incomes[*].id", "$.fixedExpenses[*].id"}) {
            for (Integer id : JsonPath.<List<Integer>>read(current, path)) {
                mvc.perform(delete("/api/ledger/" + id)).andExpect(status().isNoContent());
            }
        }
        mvc.perform(get("/api/ledger"))
                .andExpect(jsonPath("$.incomes.length()").value(0))
                .andExpect(jsonPath("$.fixedExpenses.length()").value(0))
                .andExpect(jsonPath("$.debtTotal").value(100_000_000));
    }

    @Test
    void 미래_달과_첫_기록_전_달은_비어_있고_채우지_않는다() throws Exception {
        add("INCOME", now, "월급", 3_000_000);

        mvc.perform(get("/api/ledger").param("month", now.plusMonths(1).toString()))
                .andExpect(jsonPath("$.incomes.length()").value(0))
                .andExpect(jsonPath("$.trend[10].income").value(3_000_000));
        mvc.perform(get("/api/ledger").param("month", now.minusMonths(3).toString()))
                .andExpect(jsonPath("$.incomes.length()").value(0));
        mvc.perform(get("/api/ledger").param("month", now.toString()))
                .andExpect(jsonPath("$.trend[8].income").value(0));
    }

    @Test
    void 빈_달에_처음_입력하면_이전_달을_이월한_뒤_추가() throws Exception {
        add("INCOME", now.minusMonths(1), "월급", 3_000_000);
        add("INCOME", now, "상여", 1_000_000);

        mvc.perform(get("/api/ledger"))
                .andExpect(jsonPath("$.incomes.length()").value(2))
                .andExpect(jsonPath("$.incomeTotal").value(4_000_000));
    }

    @Test
    void 수정은_구분을_바꾸지_않고_삭제() throws Exception {
        Integer id = add("FIXED_EXPENSE", now, "넷플릭스", 13_900);

        mvc.perform(put("/api/ledger/" + id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"kind":"INCOME","name":" 넷플릭스 프리미엄 ","amount":17000,"memo":" "}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("FIXED_EXPENSE"))
                .andExpect(jsonPath("$.name").value("넷플릭스 프리미엄"))
                .andExpect(jsonPath("$.memo").value(nullValue()));

        mvc.perform(delete("/api/ledger/" + id)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/ledger/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void 잘못된_입력은_400() throws Exception {
        mvc.perform(post("/api/ledger").contentType(MediaType.APPLICATION_JSON).content("""
                        {"kind":"INCOME","month":"%s","name":"월급","amount":0}""".formatted(now)))
                .andExpect(status().isBadRequest());
        // 수입·고정지출은 달이 필요
        mvc.perform(post("/api/ledger").contentType(MediaType.APPLICATION_JSON).content("""
                        {"kind":"INCOME","name":"월급","amount":1000}"""))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/ledger").contentType(MediaType.APPLICATION_JSON).content("""
                        {"kind":"INCOME","month":"2026-13","name":"월급","amount":1000}"""))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/ledger").param("month", "2026-13")).andExpect(status().isBadRequest());
    }
}
