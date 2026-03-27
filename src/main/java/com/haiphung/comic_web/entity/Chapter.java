package com.haiphung.comic_web.entity;



import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "chapters")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Chapter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chapter_id")
    private Integer chapterId;

    @ManyToOne
    @JoinColumn(name = "comic_id")
    private Comic comic;

    @Column(name = "chapter_number", precision = 5, scale = 2)
    private BigDecimal chapterNum;

    private String title;

    @Column(name = "image_count")
    private Integer imageCount = 0;

    @Column(name = "resource_path")
    private String resourcePath;

    private Integer views = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
