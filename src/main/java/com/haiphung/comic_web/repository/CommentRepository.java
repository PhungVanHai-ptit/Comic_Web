package com.haiphung.comic_web.repository;

import com.haiphung.comic_web.entity.Comment;
import com.haiphung.comic_web.entity.Comic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Integer> {

    Page<Comment> findByComicOrderByCreatedAtDesc(Comic comic, Pageable pageable);

    Page<Comment> findByContentContainingIgnoreCase(String content, Pageable pageable);

    @Modifying
    @Query("DELETE FROM Comment c WHERE c.commentId = :commentId AND c.user.userId = :userId")
    void deleteByCommentIdAndUserId(@Param("commentId") Integer commentId, @Param("userId") Integer userId);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.comic.comicId = :comicId")
    Long countByComicId(@Param("comicId") Integer comicId);
}