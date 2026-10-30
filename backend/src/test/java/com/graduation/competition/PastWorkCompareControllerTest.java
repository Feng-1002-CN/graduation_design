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
class PastWorkCompareControllerTest {

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
    void findsPastWorkWithOverlapExplanationAndSource() throws Exception {
        String content = "基于深度学习卷积神经网络的计算机视觉中药材图像识别系统。";
        DocumentEntity document = new DocumentEntity();
        document.setTitle("基于深度学习的植物图像识别系统");
        document.setFileType("PDF");
        document.setFileUrl("minio://past/work.pdf");
        document.setSourceUrl("https://example.com/past-work.pdf");
        document.setSourceDate(LocalDate.of(2025, 6, 1));
        document.setStatus("READY");
        document.setChunkCount(1);
        document.setDocumentType("PAST_WORK");
        document.setWorkYear("2025");
        document = documentRepository.save(document);

        DocumentChunk chunk = new DocumentChunk();
        chunk.setDocumentId(document.getId());
        chunk.setChunkIndex(0);
        chunk.setContent(content);
        chunk.setPageNumber(3);
        chunk.setTokenCount(30);
        chunk.setVectorId(document.getId() + "-0");
        chunk.setEmbedding(objectMapper.writeValueAsString(
                embeddingClient.embed(List.of(content)).get(0)));
        chunkRepository.save(chunk);

        mockMvc.perform(post("/api/v1/compare/past-works")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "topic":"基于计算机视觉的中药材图像识别系统",
                                  "topK":5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.works[0].title").value("基于深度学习的植物图像识别系统"))
                .andExpect(jsonPath("$.data.works[0].workYear").value("2025"))
                .andExpect(jsonPath("$.data.works[0].topicOverlap").isNotEmpty())
                .andExpect(jsonPath("$.data.works[0].techOverlap").isNotEmpty())
                .andExpect(jsonPath("$.data.works[0].source")
                        .value("https://example.com/past-work.pdf"));
    }
}

