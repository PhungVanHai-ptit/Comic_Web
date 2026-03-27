package com.haiphung.comic_web.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "comics")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Comic {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comic_id")
    private Integer comicId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 100)
    private String author;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_image", length = 500)
    private String coverImage;

    @Column(name = "status")
    private String status;

    @Column(name = "total_views")
    private Integer totalViews = 0;

    @Column(name = "total_follows")
    private Integer totalFollows = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_chapter_at")
    private LocalDateTime lastChapterAt;

    // Quan hệ với Chapter (1 truyện có nhiều chương)
    @JsonIgnore
    @OneToMany(mappedBy = "comic", cascade = CascadeType.ALL)
    @OrderBy("chapterNum DESC")
    private List<Chapter> chapters;

    // Quan hệ với Genre (Bảng trung gian comic_genre)
    @ManyToMany
    @JoinTable(
            name = "comic_genre",
            joinColumns = @JoinColumn(name = "comic_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private List<Genre> genres;

    @JsonIgnore
    @OneToMany(mappedBy = "comic", cascade = CascadeType.ALL)
    private List<Comment> comments;

    @JsonIgnore
    @OneToMany(mappedBy = "comic", cascade = CascadeType.ALL)
    private List<Follow> follows;

    @PrePersist
    protected void onCreate() {
        if (lastChapterAt == null) {
            lastChapterAt = LocalDateTime.now();
        }
    }
}