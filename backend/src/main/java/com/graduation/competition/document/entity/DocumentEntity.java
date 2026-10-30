package com.graduation.competition.document.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * 知识库文档。文件记录与解析状态分离，上传后可异步完成处理。
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "document")
public class DocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long competitionId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 16)
    private String fileType;

    @Column(nullable = false, length = 512)
    private String fileUrl;

    private String originalFilename;
    private String sourceUrl;
    private LocalDate sourceDate;

    @Column(nullable = false, length = 16)
    private String status = "PENDING";

    @Column(nullable = false)
    private int chunkCount;

    @Column(columnDefinition = "TEXT")
    private String errorMsg;

    @Column(nullable = false, length = 16)
    private String documentType = "MATERIAL";

    private String workYear;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}

