package com.haiphung.comic_web.service;

import com.haiphung.comic_web.dto.CommentResponseDTO;
import com.haiphung.comic_web.entity.Comment;
import com.haiphung.comic_web.entity.Comic;
import com.haiphung.comic_web.entity.User;
import com.haiphung.comic_web.repository.CommentRepository;
import com.haiphung.comic_web.repository.ComicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final ComicRepository comicRepository;

    @Transactional
    public Map<String, Object> addComment(Integer comicId, String content, User user) {
        Map<String, Object> response = new HashMap<>();

        Comic comic = comicRepository.findById(comicId).orElse(null);
        if (comic == null) {
            response.put("success", false);
            response.put("message", "Truyện không tồn tại");
            return response;
        }

        if (content == null || content.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Nội dung bình luận không được để trống");
            return response;
        }

        Comment comment = new Comment();
        comment.setUser(user);
        comment.setComic(comic);
        comment.setContent(content.trim());
        commentRepository.save(comment);

        response.put("success", true);
        response.put("message", "Bình luận thành công");
        return response;
    }

    @Transactional
    public Map<String, Object> deleteComment(Integer commentId, User user) {
        Map<String, Object> response = new HashMap<>();

        Comment comment = commentRepository.findById(commentId).orElse(null);
        if (comment == null) {
            response.put("success", false);
            response.put("message", "Bình luận không tồn tại");
            return response;
        }

        boolean isAdmin = user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole().getRoleName());
        if (!comment.getUser().getUserId().equals(user.getUserId()) && !isAdmin) {
            response.put("success", false);
            response.put("message", "Bạn không có quyền xóa bình luận này");
            return response;
        }

        commentRepository.delete(comment);
        response.put("success", true);
        response.put("message", "Đã xóa bình luận");
        return response;
    }

    public Map<String, Object> getComments(Integer comicId, int page) {
        Map<String, Object> response = new HashMap<>();

        Comic comic = comicRepository.findById(comicId).orElse(null);
        if (comic == null) {
            response.put("success", false);
            response.put("message", "Truyện không tồn tại");
            return response;
        }

        Pageable pageable = Pageable.ofSize(10).withPage(page);
        Page<Comment> commentsPage = commentRepository.findByComicOrderByCreatedAtDesc(comic, pageable);

        List<CommentResponseDTO> comments = commentsPage.getContent().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        response.put("success", true);
        response.put("comments", comments);
        response.put("currentPage", page);
        response.put("totalPages", commentsPage.getTotalPages());
        response.put("totalElements", commentsPage.getTotalElements());
        return response;
    }

    // ===== Admin Operations =====
    public Map<String, Object> getAllCommentsForAdmin(int page, int size, String keyword) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Comment> comments;
            
            if (keyword != null && !keyword.trim().isEmpty()) {
                comments = commentRepository.findByContentContainingIgnoreCase(keyword.trim(), pageable);
            } else {
                comments = commentRepository.findAll(pageable);
            }
            
            response.put("success", true);
            response.put("comments", comments.getContent());
            response.put("currentPage", page);
            response.put("totalPages", comments.getTotalPages());
            response.put("totalElements", comments.getTotalElements());
            response.put("keyword", keyword);
            
            return response;
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi khi tải danh sách bình luận: " + e.getMessage());
            return response;
        }
    }

    @Transactional
    public Map<String, Object> deleteCommentAsAdmin(Integer commentId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (!commentRepository.existsById(commentId)) {
                response.put("success", false);
                response.put("message", "Không tìm thấy bình luận");
                return response;
            }
            
            commentRepository.deleteById(commentId);
            response.put("success", true);
            response.put("message", "Xóa bình luận thành công!");
            
            return response;
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi khi xóa bình luận: " + e.getMessage());
            return response;
        }
    }

    private CommentResponseDTO mapToDTO(Comment comment) {
        User user = comment.getUser();
        return CommentResponseDTO.builder()
                .commentId(comment.getCommentId())
                .content(comment.getContent())
                .authorName(user != null ? user.getFullName() : "Unknown")
                .authorAvatar(user != null ? user.getAvatarUrl() : null)
                .authorId(user != null ? user.getUserId() : null)
                .createdAt(comment.getCreatedAt())
                .build();
    }
}