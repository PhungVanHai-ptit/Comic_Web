package com.haiphung.comic_web.repository;

import com.haiphung.comic_web.entity.Comic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
@Repository
public interface ComicRepository extends JpaRepository<Comic, Integer> {
    List<Comic> findTop5ByOrderByTotalViewsDesc();
    List<Comic> findTop12ByOrderByLastChapterAtDesc();
    List<Comic> findByTitleContainingIgnoreCase(String name);
    List<Comic> findByStatus(String status);
    List<Comic> findByAuthor(String author);
    List<Comic> findByGenres_Name(String name);
    Page<Comic> findByGenres_GenreId(Integer genreId, Pageable pageable);
    Boolean existsByTitleIgnoreCase(String name);

    boolean existsByTitleIgnoreCaseAndComicIdNot(String name, Integer id);

    @Query("SELECT DISTINCT c FROM Comic c LEFT JOIN c.genres g WHERE " +
           "LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.author) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.status) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(g.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Comic> searchComics(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT DISTINCT c FROM Comic c LEFT JOIN c.genres g WHERE " +
           "(:keyword IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.author) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:status IS NULL OR c.status = :status) AND " +
           "(:genreId IS NULL OR g.genreId = :genreId)")
    Page<Comic> searchAdvanced(@Param("keyword") String keyword,
                               @Param("status") String status,
                               @Param("genreId") Integer genreId,
                               Pageable pageable);
}
