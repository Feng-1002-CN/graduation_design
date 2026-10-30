package com.graduation.competition.rag.splitter;

import com.graduation.competition.rag.parser.ParsedPage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 采用字符数近似 token，默认 512 字符、64 字符重叠；边界优先落在句末。
 */
@Component
public class TextChunkSplitter {

    private static final int CHUNK_SIZE = 512;
    private static final int OVERLAP = 64;

    public List<TextChunk> split(List<ParsedPage> pages) {
        List<TextChunk> chunks = new ArrayList<>();
        int index = 0;
        for (ParsedPage page : pages) {
            String text = normalize(page.text());
            int cursor = 0;
            while (cursor < text.length()) {
                int end = Math.min(text.length(), cursor + CHUNK_SIZE);
                if (end < text.length()) {
                    int sentenceEnd = findSentenceBoundary(text, cursor, end);
                    if (sentenceEnd > cursor + CHUNK_SIZE / 2) {
                        end = sentenceEnd;
                    }
                }
                String content = text.substring(cursor, end).trim();
                if (!content.isBlank()) {
                    chunks.add(new TextChunk(index++, page.pageNumber(), content,
                            Math.max(1, content.length() / 3)));
                }
                if (end >= text.length()) {
                    break;
                }
                cursor = Math.max(cursor + 1, end - OVERLAP);
            }
        }
        return chunks;
    }

    private int findSentenceBoundary(String text, int start, int end) {
        for (int index = end; index > start; index--) {
            char value = text.charAt(index - 1);
            if (value == '。' || value == '！' || value == '？' || value == '\n'
                    || value == '.' || value == '!' || value == '?') {
                return index;
            }
        }
        return end;
    }

    private String normalize(String value) {
        return value.replace("\r\n", "\n")
                .replaceAll("[\\t\\x0B\\f]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}

