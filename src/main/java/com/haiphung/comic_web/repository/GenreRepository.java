package com.haiphung.comic_web.repository;

import com.haiphung.comic_web.entity.Genre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GenreRepository extends JpaRepository<Genre, Integer> {
    Optional<Genre> findByName(String name);
    boolean existsByName(String name);
    Page<Genre> findByNameContainingIgnoreCase(String name, Pageable pageable);

    //    Hàm	Ý nghĩa
    //    findByNameContainingIgnoreCase	chứa keyword (LIKE %...%)
    //    findByNameStartingWithIgnoreCase	bắt đầu bằng keyword
    //    findByNameEndingWithIgnoreCase	kết thúc bằng keyword
    //    findByNameIgnoreCase	khớp chính xác (không phân biệt hoa thường)
    //    findByName	khớp chính xác (có phân biệt hoa thường)
}
