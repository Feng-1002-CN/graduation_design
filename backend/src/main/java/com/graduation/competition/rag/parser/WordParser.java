package com.graduation.competition.rag.parser;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DOCX 段落解析。Word 无稳定分页信息，因此以逻辑页 1 标注。
 */
@Component
public class WordParser implements DocumentParser {

    @Override
    public boolean supports(String extension) {
        return "docx".equalsIgnoreCase(extension);
    }

    @Override
    public List<ParsedPage> parse(byte[] content) throws Exception {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(content))) {
            String text = document.getParagraphs().stream()
                    .map(paragraph -> paragraph.getText().trim())
                    .filter(value -> !value.isBlank())
                    .collect(Collectors.joining("\n"));
            return text.isBlank() ? List.of() : List.of(new ParsedPage(1, text));
        }
    }
}

