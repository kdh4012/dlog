package com.dlog.diary;

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
class DiaryApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    DiaryRepository repository;

    @AfterEach
    void cleanUp() {
        repository.deleteAll();
    }

    private Integer write(String date, String title, String content) throws Exception {
        String titleJson = title == null ? "null" : "\"" + title + "\"";
        String body = mvc.perform(post("/api/diary").contentType(MediaType.APPLICATION_JSON).content("""
                        {"entryDate":"%s","title":%s,"content":"%s"}""".formatted(date, titleJson, content)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void 날짜_최근_순_목록과_본문_미리보기() throws Exception {
        write("2026-11-19", "출근", "평범한 하루");
        Integer second = write("2026-11-21", null, "# 주말\\n\\n- **산책** 했다\\n- 커피");
        write("2026-11-21", "저녁", "같은 날 두 번째");

        mvc.perform(get("/api/diary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].title").value("저녁")) // 같은 날이면 나중에 쓴 순
                .andExpect(jsonPath("$.content[1].id").value(second))
                .andExpect(jsonPath("$.content[1].title").value(nullValue()))
                .andExpect(jsonPath("$.content[1].preview").value("주말 산책 했다 커피"))
                .andExpect(jsonPath("$.content[2].entryDate").value("2026-11-19"));

        mvc.perform(get("/api/diary").param("size", "2").param("page", "1"))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void 조회_수정_삭제() throws Exception {
        Integer id = write("2026-11-20", "초안", "본문");

        mvc.perform(get("/api/diary/" + id))
                .andExpect(jsonPath("$.content").value("본문"))
                .andExpect(jsonPath("$.entryDate").value("2026-11-20"));

        mvc.perform(put("/api/diary/" + id).contentType(MediaType.APPLICATION_JSON).content("""
                        {"entryDate":"2026-11-21","title":"  ","content":"고친 본문"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryDate").value("2026-11-21"))
                .andExpect(jsonPath("$.title").value(nullValue()))
                .andExpect(jsonPath("$.content").value("고친 본문"));

        mvc.perform(delete("/api/diary/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/diary/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void 날짜와_본문은_필수() throws Exception {
        mvc.perform(post("/api/diary").contentType(MediaType.APPLICATION_JSON).content("""
                        {"content":"본문"}"""))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/diary").contentType(MediaType.APPLICATION_JSON).content("""
                        {"entryDate":"2026-11-21","content":"  "}"""))
                .andExpect(status().isBadRequest());
    }
}
