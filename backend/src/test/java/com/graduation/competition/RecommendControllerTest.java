package com.graduation.competition;

import com.graduation.competition.entity.Competition;
import com.graduation.competition.entity.CompetitionGroup;
import com.graduation.competition.repository.CompetitionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class RecommendControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompetitionRepository competitionRepository;

    @Test
    void recommendsOpenCompetitionWithSourceAndExcludesExpired() throws Exception {
        competitionRepository.save(competition("计算机视觉创新挑战赛", "计算机视觉与机器学习作品",
                "本科生组", LocalDate.now().plusMonths(4)));
        competitionRepository.save(competition("已结束的视觉比赛", "计算机视觉",
                "本科生组", LocalDate.now().minusDays(1)));

        mockMvc.perform(post("/api/v1/search/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "grade":"大二",
                                  "direction":"计算机视觉",
                                  "keywords":["机器学习","图像识别"],
                                  "limit":10
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recommendations[0].name").value("计算机视觉创新挑战赛"))
                .andExpect(jsonPath("$.data.recommendations[0].source")
                        .value("https://example.com/competition-notice"))
                .andExpect(jsonPath("$.data.recommendations[0].sourceDate").exists())
                .andExpect(jsonPath("$.data.recommendations[0].matchedKeywords").isArray());
    }

    private Competition competition(String name, String description, String group, LocalDate end) {
        Competition competition = new Competition();
        competition.setName(name);
        competition.setDescription(description);
        competition.setLevel("国家级");
        competition.setSourceUrl("https://example.com/competition-notice");
        competition.setSourceDate(LocalDate.now().minusDays(10));
        competition.setContestEnd(end);
        competition.setRegisterEnd(end);
        CompetitionGroup competitionGroup = new CompetitionGroup();
        competitionGroup.setName(group);
        competitionGroup.setEligibility("全日制本科生，大二及以上可报名");
        competition.addGroup(competitionGroup);
        return competition;
    }
}

