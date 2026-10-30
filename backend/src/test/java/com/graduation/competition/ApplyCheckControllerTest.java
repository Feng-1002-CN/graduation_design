package com.graduation.competition;

import com.graduation.competition.entity.Competition;
import com.graduation.competition.entity.CompetitionRule;
import com.graduation.competition.repository.CompetitionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class ApplyCheckControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompetitionRepository competitionRepository;

    @Test
    void extractsRuleMaterialsAndReportsMissingItems() throws Exception {
        Competition competition = new Competition();
        competition.setName("申报材料测试竞赛");
        competition.setSourceUrl("https://example.com/apply-rules");
        competition.setSourceDate(LocalDate.now().minusDays(10));
        competition.setContestEnd(LocalDate.now().plusMonths(2));
        CompetitionRule rule = new CompetitionRule();
        rule.setTitle("报名材料");
        rule.setContent("报名时须提交报名表、学生证复印件和作品说明。");
        rule.setSourceUrl("https://example.com/apply-rules#materials");
        rule.setSourceDate(LocalDate.now().minusDays(10));
        competition.addRule(rule);
        competition = competitionRepository.save(competition);

        String payload = """
                {
                  "competitionId": %d,
                  "files":[{"name":"报名表.pdf","type":"application/pdf","url":"minio://apply/form.pdf"}]
                }
                """.formatted(competition.getId());
        String response = mockMvc.perform(post("/api/v1/apply/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.required[0].source")
                        .value("https://example.com/apply-rules#materials"))
                .andExpect(jsonPath("$.data.result[?(@.status == 'OK')]").isNotEmpty())
                .andExpect(jsonPath("$.data.result[?(@.status == 'MISSING')]").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        long id = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(response).path("data").path("id").asLong();
        mockMvc.perform(get("/api/v1/apply/check/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id));
    }
}

