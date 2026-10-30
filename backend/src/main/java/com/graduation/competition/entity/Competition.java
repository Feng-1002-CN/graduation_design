package com.graduation.competition.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 竞赛主记录。来源与来源日期用于过期规则判定和证据展示。
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "competition")
public class Competition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String organizer;
    private String category;
    private String level;
    private String officialWebsite;
    private LocalDate registerStart;
    private LocalDate registerEnd;
    private LocalDate contestStart;
    private LocalDate contestEnd;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 512)
    private String sourceUrl;

    @Column(nullable = false)
    private LocalDate sourceDate;

    @Column(nullable = false, length = 16)
    private String status = "ACTIVE";

    @Column(nullable = false)
    private boolean deleted;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @BatchSize(size = 50)
    @OneToMany(mappedBy = "competition", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<CompetitionGroup> groups = new ArrayList<>();

    @BatchSize(size = 50)
    @OneToMany(mappedBy = "competition", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<CompetitionRule> rules = new ArrayList<>();

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void addGroup(CompetitionGroup group) {
        groups.add(group);
        group.setCompetition(this);
    }

    public void addRule(CompetitionRule rule) {
        rules.add(rule);
        rule.setCompetition(this);
    }
}

