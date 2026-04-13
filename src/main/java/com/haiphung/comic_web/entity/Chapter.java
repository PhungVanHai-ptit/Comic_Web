package com.haiphung.comic_web.entity;



import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import com.haiphung.comic_web.config.StringListConverter;

import java.util.ArrayList;
import java.util.List;

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

    
//    @Deprecated
//    @Column(name = "image_count")
//    private Integer imageCount = 0;
//
//    @Deprecated
//    @Column(name = "resource_path")
//    private String resourcePath;

    @Convert(converter = StringListConverter.class)
    @Column(name = "image_paths", columnDefinition = "JSON")
    private List<String> imagePaths = new ArrayList<>();

    @Column(name = "require_login", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean requireLogin = false;

    private Integer views = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
