package com.graduation.competition.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI 元数据。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI competitionOpenApi() {
        return new OpenAPI().info(new Info()
                .title("学科竞赛智能辅助系统 API")
                .description("RAG 知识库、四步选题分析智能体与多模态内容生成")
                .version("1.0.0"));
    }
}

