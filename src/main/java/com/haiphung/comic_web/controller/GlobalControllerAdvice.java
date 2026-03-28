package com.haiphung.comic_web.controller;

import com.haiphung.comic_web.service.GenreService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private final GenreService genreService;

    @Value("${minio.url}")
    private String minioUrl;

    @ModelAttribute("minioUrl")
    public String getMinioUrl() {
        return minioUrl;
    }

    @ModelAttribute("allGenres")
    public List<?> getAllGenres() {
        return genreService.getAllGenresSorted();
    }
}
