package com.haiphung.comic_web.service;

import com.haiphung.comic_web.entity.Comic;
import com.haiphung.comic_web.repository.ComicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ComicService {

    private final ComicRepository comicRepository;

    public Page<Comic> getAllComics(Pageable pageable) {
        return comicRepository.findAll(pageable);
    }

    public Page<Comic> getComicsByGenre(Integer genreId, Pageable pageable) {
        return comicRepository.findByGenres_GenreId(genreId, pageable);
    }

    public List<Comic> getTop5ByViews() {
        return comicRepository.findTop5ByOrderByTotalViewsDesc();
    }

    public List<Comic> getLatestComics() {
        return comicRepository.findTop12ByOrderByLastChapterAtDesc();
    }

    public Optional<Comic> getComicById(Integer id) {
        return comicRepository.findById(id);
    }

    @Transactional
    public Comic saveComic(Comic comic) {
        return comicRepository.save(comic);
    }

    @Transactional
    public void deleteComic(Integer id) {
        comicRepository.deleteById(id);
    }

    public boolean existsByTitle(String title) {
        return comicRepository.existsByTitleIgnoreCase(title);
    }

    public boolean existsByTitleAndIdNot(String title, Integer id) {
        return comicRepository.existsByTitleIgnoreCaseAndComicIdNot(title, id);
    }

    public Page<Comic> searchAdvanced(String keyword, String status, Integer genreId, Pageable pageable) {
        String kw = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        String st = (status != null && !status.trim().isEmpty()) ? status.trim() : null;
        
        if (kw == null && st == null && genreId == null) {
            return comicRepository.findAll(pageable);
        }
        return comicRepository.searchAdvanced(kw, st, genreId, pageable);
    }

    @Transactional
    public void touchComic(Integer comicId) {
        comicRepository.findById(comicId).ifPresent(comic -> {
            comic.setUpdatedAt(java.time.LocalDateTime.now());
            comicRepository.save(comic);
        });
    }

    @Transactional
    public void incrementViews(Integer comicId) {
        comicRepository.findById(comicId).ifPresent(comic -> {
            comic.setTotalViews((comic.getTotalViews() == null ? 0 : comic.getTotalViews()) + 1);
            comicRepository.save(comic);
        });
    }

    @Transactional
    public void updateLastChapterAt(Integer comicId) {
        comicRepository.findById(comicId).ifPresent(comic -> {
            Optional<com.haiphung.comic_web.entity.Chapter> latestOpt = comic.getChapters().stream()
                    .max(java.util.Comparator.comparing(com.haiphung.comic_web.entity.Chapter::getCreatedAt));
            
            if (latestOpt.isPresent()) {
                comic.setLastChapterAt(latestOpt.get().getCreatedAt());
            } else {
                comic.setLastChapterAt(comic.getCreatedAt());
            }
            comic.setUpdatedAt(java.time.LocalDateTime.now());
            comicRepository.save(comic);
        });
    }

    @Transactional
    public void initializeLastChapterAt() {

        List<Comic> comics = comicRepository.findAll();
        for (Comic comic : comics) {
            Optional<com.haiphung.comic_web.entity.Chapter> latestOpt = comic.getChapters().stream()
                    .max(java.util.Comparator.comparing(com.haiphung.comic_web.entity.Chapter::getCreatedAt));
            
            if (latestOpt.isPresent()) {
                comic.setLastChapterAt(latestOpt.get().getCreatedAt());
            } else {
                comic.setLastChapterAt(comic.getCreatedAt());
            }
        }
        comicRepository.saveAll(comics);
    }
}
