package com.graduation.competition;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 学科竞赛智能辅助系统启动入口。
 */
@EnableAsync
@SpringBootApplication
public class CompetitionApplication {

    public static void main(String[] args) {
        SpringApplication.run(CompetitionApplication.class, args);
    }
}

