package com.graduation.competition.rag.parser;

import java.util.List;

public interface DocumentParser {

    boolean supports(String extension);

    List<ParsedPage> parse(byte[] content) throws Exception;
}

