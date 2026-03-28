package com.haiphung.comic_web.service;

import com.haiphung.comic_web.entity.Genre;
import com.haiphung.comic_web.repository.GenreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GenreService {

    private final GenreRepository genreRepository;

    public Page<Genre> getAllGenres(Pageable pageable) {
        return genreRepository.findAll(pageable);
    }


    public List<Genre> getGenresByIds(List<Integer> ids) {
        return genreRepository.findAllById(ids);
    }

    @Cacheable("allGenres")
    public List<Genre> getAllGenresSorted() {
        return genreRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    public Page<Genre> searchGenresByName(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return genreRepository.findByNameContainingIgnoreCase(keyword.trim(), pageable);
        }
        return genreRepository.findAll(pageable);
    }
    //    Hàm	Ý nghĩa
    //    findByNameContainingIgnoreCase	chứa keyword (LIKE %...%)
    //    findByNameStartingWithIgnoreCase	bắt đầu bằng keyword
    //    findByNameEndingWithIgnoreCase	kết thúc bằng keyword
    //    findByNameIgnoreCase	khớp chính xác (không phân biệt hoa thường)
    //    findByName	khớp chính xác (có phân biệt hoa thường)

    public Optional<Genre> getGenreById(Integer id) {
        return genreRepository.findById(id);
    }

    @Transactional
    @CacheEvict(value = "allGenres", allEntries = true)
    public Genre saveGenre(Genre genre) {
        return genreRepository.save(genre);
    }

    @Transactional
    @CacheEvict(value = "allGenres", allEntries = true)
    public void deleteGenre(Integer id) {
        genreRepository.deleteById(id);
    }

    /**
     * Kiểm tra tên thể loại đã tồn tại hay không
     * ✅ Xử lý null và khoảng trắng
     */
    public boolean existsByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        return genreRepository.existsByName(name.trim());
    }

    /**
     * Kiểm tra tên thể loại có tồn tại (ngoại trừ ID hiện tại) hay không
     * ✅ Xử lý null, khoảng trắng và ID null
     */
    public boolean existsByNameAndIdNot(String name, Integer id) {
        if (name == null || name.trim().isEmpty() || id == null) {
            return false;
        }
        return genreRepository.findByName(name.trim())
                .map(g -> !g.getGenreId().equals(id))
                .orElse(false);
    }
}