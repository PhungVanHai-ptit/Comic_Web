package com.haiphung.comic_web.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "follows")
@Getter
@Setter
@NoArgsConstructor @AllArgsConstructor
public class Follow {

    @EmbeddedId
    private FollowId id = new FollowId(); // Khóa phức hợp (User_ID + Comic_ID)

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @MapsId("comicId")
    @JoinColumn(name = "comic_id")
    private Comic comic;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}

