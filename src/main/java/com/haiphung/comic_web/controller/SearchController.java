package com.haiphung.comic_web.controller;

import com.haiphung.comic_web.entity.Comic;
import com.haiphung.comic_web.repository.ComicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class SearchController {

    private final ComicRepository comicRepository;

    @Value("${minio.url}")
    private String minioUrl;

    @GetMapping("/search")
    public String search(@RequestParam(value = "keyword", defaultValue = "") String keyword,
                         @RequestParam(value = "page", defaultValue = "0") int page,
                         Model model) {
        Pageable pageable = PageRequest.of(page, 20);
        Page<Comic> results;
        
        if (keyword == null || keyword.trim().isEmpty()) {
            results = Page.empty(pageable);
        } else {
            results = comicRepository.searchComics(keyword.trim(), pageable);
        }
        
        model.addAttribute("keyword", keyword);
        model.addAttribute("comics", results.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", results.getTotalPages());
        model.addAttribute("totalElements", results.getTotalElements());
        model.addAttribute("minioUrl", minioUrl);
        
        return "search-results";
    }
}
