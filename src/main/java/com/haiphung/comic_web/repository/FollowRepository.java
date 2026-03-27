package com.haiphung.comic_web.repository;

import com.haiphung.comic_web.entity.Comic;
import com.haiphung.comic_web.entity.Follow;
import com.haiphung.comic_web.entity.FollowId;
import com.haiphung.comic_web.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FollowRepository extends JpaRepository<Follow, FollowId> {

    Optional<Follow> findByUserAndComic(User user, Comic comic);

    boolean existsByUserAndComic(User user, Comic comic);

    @Query("SELECT f.comic FROM Follow f WHERE f.user.userId = :userId")
    Page<Comic> findComicsByUserId(@Param("userId") Integer userId, Pageable pageable);

    @Query("SELECT COUNT(f) FROM Follow f WHERE f.comic.comicId = :comicId")
    Long countByComicId(@Param("comicId") Integer comicId);

    @Modifying
    @Query("DELETE FROM Follow f WHERE f.user.userId = :userId AND f.comic.comicId = :comicId")
    void deleteByUserIdAndComicId(@Param("userId") Integer userId, @Param("comicId") Integer comicId);
}