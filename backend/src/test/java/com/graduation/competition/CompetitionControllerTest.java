package com.graduation.competition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class CompetitionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createsAndQueriesCompetitionWithChildren() throws Exception {
        String payload = """
                {
                  "name": "中国大学生计算机设计大赛",
                  "organizer": "教育部高等学校计算机类专业教学指导委员会",
                  "category": "软件",
                  "level": "国家级",
                  "officialWebsite": "https://jsjds.example.edu.cn",
                  "registerStart": "2026-01-01",
                  "registerEnd": "2026-04-30",
                  "contestStart": "2026-05-01",
                  "contestEnd": "2027-08-31",
                  "sourceUrl": "https://jsjds.example.edu.cn/notice",
                  "sourceDate": "2026-01-01",
                  "groups": [{"name": "本科生组", "eligibility": "全日制本科生"}],
                  "rules": [{
                    "title": "评分标准",
                    "content": "创新性 30%，技术实现 40%，完整性 30%。",
                    "sourceUrl": "https://jsjds.example.edu.cn/rules",
                    "sourceDate": "2026-01-01",
                    "publishDate": "2026-01-01"
                  }]
                }
                """;

        String response = mockMvc.perform(post("/api/v1/competitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.groups[0].name").value("本科生组"))
                .andExpect(jsonPath("$.data.rules[0].title").value("评分标准"))
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        long id = json.path("data").path("id").asLong();
        mockMvc.perform(get("/api/v1/competitions")
                        .param("name", "计算机设计")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].sourceUrl").exists());

        mockMvc.perform(get("/api/v1/competitions/{id}/rules", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].sourceDate").value("2026-01-01"));
    }

    @Test
    void rejectsMissingSourceMetadata() throws Exception {
        mockMvc.perform(post("/api/v1/competitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"无来源竞赛"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40002))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
}

