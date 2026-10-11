package com.dlog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** dlog.admin.auto-login=true(로컬 개발용)면 토큰 없이도 관리자 API 에 접근된다 */
@SpringBootTest(properties = "dlog.admin.auto-login=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AutoLoginApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void 자동_로그인시_토큰없이_관리자() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"));
        mvc.perform(get("/api/assets")).andExpect(status().isOk());
    }

    @Test
    void 자동_로그인시에도_잘못된_토큰은_관리자로_대체() throws Exception {
        // 만료·위조 토큰이 남아 있어도 로컬에서는 관리자 화면이 막히지 않아야 함
        mvc.perform(get("/api/assets").header("Authorization", "Bearer invalid"))
                .andExpect(status().isOk());
    }
}
