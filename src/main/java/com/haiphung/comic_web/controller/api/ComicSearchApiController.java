package com.haiphung.comic_web.controller.api;

import com.haiphung.comic_web.dto.SearchResultDto;
import com.haiphung.comic_web.entity.Comic;
import com.haiphung.comic_web.repository.ComicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/comics")
@RequiredArgsConstructor
public class ComicSearchApiController {

    private final ComicRepository comicRepository;

    @GetMapping("/search")
    public List<SearchResultDto> search(@RequestParam(value = "q", defaultValue = "") String q) {
        if (q == null || q.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<Comic> comics = comicRepository.searchComics(q.trim(), PageRequest.of(0, 4)).getContent();
        
        return comics.stream().map(this::toDto).collect(Collectors.toList());
    }
    
    private SearchResultDto toDto(Comic comic) {
        SearchResultDto dto = new SearchResultDto();
        dto.setComicId(comic.getComicId());
        dto.setTitle(comic.getTitle());
        dto.setCoverImage(comic.getCoverImage());
        dto.setTotalViews(comic.getTotalViews());
        dto.setTotalFollows(comic.getTotalFollows());
        
        if (comic.getChapters() != null && !comic.getChapters().isEmpty()) {
            var latestChapter = comic.getChapters().stream()
                    .max(Comparator.comparing(c -> c.getUpdatedAt() != null ? c.getUpdatedAt() : c.getCreatedAt()))
                    .orElse(null);
            
            if (latestChapter != null) {
                dto.setLatestChapterId(latestChapter.getChapterId());
                dto.setLatestChapterNum(latestChapter.getChapterNum());
                dto.setLatestChapterTitle(latestChapter.getTitle());
                dto.setLatestChapterUpdatedAt(latestChapter.getUpdatedAt() != null ? 
                        latestChapter.getUpdatedAt() : latestChapter.getCreatedAt());
            }
        }
        
        return dto;
    }
}
