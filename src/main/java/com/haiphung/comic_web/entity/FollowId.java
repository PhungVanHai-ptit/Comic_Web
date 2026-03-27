package com.haiphung.comic_web.entity;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class FollowId implements Serializable {

    private Integer userId;
    private Integer comicId;

}
