package com.haiphung.comic_web.repository;

import com.haiphung.comic_web.entity.Chapter;
import com.haiphung.comic_web.entity.Comic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Integer> {
    List<Chapter> findByComicOrderByChapterNumDesc(Comic comic);
    
    Page<Chapter> findByComic(Comic comic, Pageable pageable);
    
    List<Chapter> findByComicAndChapterNumBetweenOrderByChapterNumDesc(Comic comic, BigDecimal start, BigDecimal end);

    Optional<Chapter> findTopByComicOrderByCreatedAtDesc(Comic comic);
}
