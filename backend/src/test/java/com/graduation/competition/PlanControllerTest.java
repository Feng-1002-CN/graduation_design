package com.graduation.competition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graduation.competition.project.entity.Project;
import com.graduation.competition.project.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class PlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectRepository projectRepository;

    @Test
    void generatesPersistsAndUpdatesStructuredPlan() throws Exception {
        Project project = new Project();
        project.setTitle("基于计算机视觉的中药材识别系统");
        project.setDescription("图像识别、人工复核与移动端演示");
        project = projectRepository.save(project);

        String created = mockMvc.perform(post("/api/v1/plans/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":" + project.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.modules").isArray())
                .andExpect(jsonPath("$.data.modules[0].name").isNotEmpty())
                .andExpect(jsonPath("$.data.techRoute").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        JsonNode data = objectMapper.readTree(created).path("data");
        long id = data.path("id").asLong();
        mockMvc.perform(get("/api/v1/plans/{projectId}", project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id));

        String updateBody = data.deepCopy().toString();
        mockMvc.perform(put("/api/v1/plans/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.projectId").value(project.getId()))
                .andExpect(jsonPath("$.data.modules[0].description").isNotEmpty());
    }
}

