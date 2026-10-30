package com.graduation.competition.document.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * 文档切片。embedding 仅用于本地降级向量检索，生产环境同步写入 Milvus。
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "document_chunk",
        indexes = @Index(name = "idx_chunk_document", columnList = "document_id,chunk_index"))
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Lob
    @Column(nullable = false)
    private String content;

    private Integer pageNumber;
    private Integer tokenCount;

    @Column(length = 128)
    private String vectorId;

    @Lob
    private String embedding;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}

