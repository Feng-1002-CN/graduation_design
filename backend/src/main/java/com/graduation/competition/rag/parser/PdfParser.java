package com.graduation.competition.rag.parser;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 使用 PDFBox 逐页提取文本，保留原始页码用于证据链。
 */
@Component
public class PdfParser implements DocumentParser {

    @Override
    public boolean supports(String extension) {
        return "pdf".equalsIgnoreCase(extension);
    }

    @Override
    public List<ParsedPage> parse(byte[] content) throws Exception {
        try (PDDocument document = Loader.loadPDF(content)) {
            List<ParsedPage> pages = new ArrayList<>();
            PDFTextStripper stripper = new PDFTextStripper();
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(document).trim();
                if (!text.isBlank()) {
                    pages.add(new ParsedPage(page, text));
                }
            }
            return pages;
        }
    }
}

