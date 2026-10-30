package com.graduation.competition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void parsesPdfAndReturnsTraceableSearchHits() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "rules.pdf", MediaType.APPLICATION_PDF_VALUE, samplePdf());

        String upload = mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(file)
                        .param("title", "计算机设计大赛规则")
                        .param("sourceUrl", "https://example.com/rules.pdf")
                        .param("sourceDate", "2026-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PROCESSING"))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(upload).path("data").path("documentId").asLong();

        JsonNode document = null;
        for (int attempt = 0; attempt < 30; attempt++) {
            String body = mockMvc.perform(get("/api/v1/documents/{id}", id))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            document = objectMapper.readTree(body).path("data");
            if ("READY".equals(document.path("status").asText())
                    || "FAILED".equals(document.path("status").asText())) {
                break;
            }
            Thread.sleep(100);
        }

        org.assertj.core.api.Assertions.assertThat(document).isNotNull();
        org.assertj.core.api.Assertions.assertThat(document.path("status").asText()).isEqualTo("READY");
        org.assertj.core.api.Assertions.assertThat(document.path("chunkCount").asInt()).isPositive();

        mockMvc.perform(post("/api/v1/documents/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query":"评分标准是什么","topK":3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hits[0].source").value("https://example.com/rules.pdf"))
                .andExpect(jsonPath("$.data.hits[0].sourceDate").value("2026-01-01"))
                .andExpect(jsonPath("$.data.hits[0].pageNumber").value(1));
    }

    private byte[] samplePdf() throws Exception {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(50, 700);
                content.showText("Scoring standard: innovation 30 percent, implementation 40 percent.");
                content.endText();
            }
            document.save(output);
            return output.toByteArray();
        }
    }
}

