package com.graduation.competition.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 本地演示模式：不调用大模型，仅从调用方提供的证据摘录生成明确的保守回答。
 */
@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "extractive")
public class ExtractiveAiGateway implements AiGateway {

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        int marker = userPrompt.indexOf("【证据】");
        String evidence = marker >= 0 ? userPrompt.substring(marker + 4).trim() : "未提供可展示证据。";
        return "本地演示模式未调用大模型。依据当前检索片段，可供核对的内容如下：\n\n"
                + evidence
                + "\n\n以上仅为资料摘录，请结合对应引用核验。";
    }
}

