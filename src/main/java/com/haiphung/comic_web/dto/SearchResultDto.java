package com.haiphung.comic_web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultDto {
    private Integer comicId;
    private String title;
    private String coverImage;
    private Integer totalViews;
    private Integer totalFollows;
    
    private Integer latestChapterId;
    private BigDecimal latestChapterNum;
    private String latestChapterTitle;
    private LocalDateTime latestChapterUpdatedAt;
}
