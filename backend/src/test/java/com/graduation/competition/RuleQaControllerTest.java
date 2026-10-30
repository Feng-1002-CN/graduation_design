package com.graduation.competition;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.graduation.competition.document.entity.DocumentChunk;
import com.graduation.competition.document.entity.DocumentEntity;
import com.graduation.competition.document.repository.DocumentChunkRepository;
import com.graduation.competition.document.repository.DocumentRepository;
import com.graduation.competition.rag.embedding.EmbeddingClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class RuleQaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentChunkRepository chunkRepository;

    @Autowired
    private EmbeddingClient embeddingClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void answersWithEvidenceReferences() throws Exception {
        String content = "本科生组报名必须提交报名表、学生证复印件和项目说明。";
        DocumentEntity document = new DocumentEntity();
        document.setTitle("报名要求");
        document.setFileType("PDF");
        document.setFileUrl("minio://test/rules.pdf");
        document.setSourceUrl("https://example.com/registration.pdf");
        document.setSourceDate(LocalDate.of(2026, 1, 1));
        document.setStatus("READY");
        document.setChunkCount(1);
        document = documentRepository.save(document);

        DocumentChunk chunk = new DocumentChunk();
        chunk.setDocumentId(document.getId());
        chunk.setChunkIndex(0);
        chunk.setContent(content);
        chunk.setPageNumber(2);
        chunk.setTokenCount(20);
        chunk.setVectorId(document.getId() + "-0");
        chunk.setEmbedding(objectMapper.writeValueAsString(
                embeddingClient.embed(List.of(content)).get(0)));
        chunkRepository.save(chunk);

        mockMvc.perform(post("/api/v1/rules/qa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question":"本科生组报名必须提交报名表、学生证复印件和项目说明。","topK":3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fallback").value(false))
                .andExpect(jsonPath("$.data.references[0].source")
                        .value("https://example.com/registration.pdf"))
                .andExpect(jsonPath("$.data.references[0].pageNumber").value(2))
                .andExpect(jsonPath("$.data.references[0].sourceDate").value("2026-01-01"));
    }

    @Test
    void returnsFallbackWhenNothingMatches() throws Exception {
        mockMvc.perform(post("/api/v1/rules/qa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question":"NONEXISTENT_987654321_UNRELATED_QUERY","topK":3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fallback").value(true))
                .andExpect(jsonPath("$.data.references").isEmpty());
    }
}

