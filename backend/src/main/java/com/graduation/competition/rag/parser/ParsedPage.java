package com.graduation.competition.rag.parser;

/**
 * 文档解析后的页/段落文本。
 */
public record ParsedPage(int pageNumber, String text) {
}

