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
class LedgerApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    LedgerRepository repository;

    @AfterEach
    void cleanUp() {
        repository.deleteAll();
    }

    private Integer add(String kind, String name, long amount) throws Exception {
        String body = mvc.perform(post("/api/ledger").contentType(MediaType.APPLICATION_JSON).content("""
                        {"kind":"%s","name":"%s","amount":%d}""".formatted(kind, name, amount)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void 구분별_합계와_월_잔액_금액_큰_순() throws Exception {
        add("INCOME", "배당", 200_000);
        add("INCOME", "월급", 3_500_000);
        add("FIXED_EXPENSE", "통신비", 55_000);
        add("FIXED_EXPENSE", "월세", 600_000);
        add("DEBT", "학자금", 8_000_000);
        add("DEBT", "전세대출", 100_000_000);

        mvc.perform(get("/api/ledger"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incomeTotal").value(3_700_000))
                .andExpect(jsonPath("$.fixedExpenseTotal").value(655_000))
                .andExpect(jsonPath("$.monthlyBalance").value(3_045_000))
                .andExpect(jsonPath("$.debtTotal").value(108_000_000))
                .andExpect(jsonPath("$.incomes[0].name").value("월급"))
                .andExpect(jsonPath("$.incomes[1].name").value("배당"))
                .andExpect(jsonPath("$.fixedExpenses[0].name").value("월세"))
                .andExpect(jsonPath("$.debts[0].name").value("전세대출"))
                .andExpect(jsonPath("$.debts[1].kind").value("DEBT"));
    }

    @Test
    void 비어_있으면_합계_0() throws Exception {
        mvc.perform(get("/api/ledger"))
                .andExpect(jsonPath("$.incomeTotal").value(0))
                .andExpect(jsonPath("$.monthlyBalance").value(0))
                .andExpect(jsonPath("$.debts.length()").value(0));
    }

    @Test
    void 수정은_구분을_바꾸지_않고_삭제() throws Exception {
        Integer id = add("FIXED_EXPENSE", "넷플릭스", 13_900);

        mvc.perform(put("/api/ledger/" + id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"kind":"INCOME","name":" 넷플릭스 프리미엄 ","amount":17000,"memo":" "}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("FIXED_EXPENSE"))
                .andExpect(jsonPath("$.name").value("넷플릭스 프리미엄"))
                .andExpect(jsonPath("$.amount").value(17_000))
                .andExpect(jsonPath("$.memo").value(nullValue()));
        mvc.perform(get("/api/ledger")).andExpect(jsonPath("$.fixedExpenseTotal").value(17_000));

        mvc.perform(delete("/api/ledger/" + id)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/ledger/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void 잘못된_입력은_400() throws Exception {
        mvc.perform(post("/api/ledger").contentType(MediaType.APPLICATION_JSON).content("""
                        {"kind":"INCOME","name":"월급","amount":0}"""))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/ledger").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name":"월급","amount":1000}"""))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/ledger").contentType(MediaType.APPLICATION_JSON).content("""
                        {"kind":"INCOME","name":" ","amount":1000}"""))
                .andExpect(status().isBadRequest());
    }
}
