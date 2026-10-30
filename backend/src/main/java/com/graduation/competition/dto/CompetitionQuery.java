package com.graduation.competition.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDate;

/**
 * 竞赛分页筛选条件。
 */
@Data
public class CompetitionQuery {

    @Min(1)
    private int page = 1;

    @Min(1)
    @Max(100)
    private int size = 10;

    private String name;
    private String organizer;
    private String category;
    private String level;
    private String status;
    private LocalDate dateFrom;
    private LocalDate dateTo;
}

