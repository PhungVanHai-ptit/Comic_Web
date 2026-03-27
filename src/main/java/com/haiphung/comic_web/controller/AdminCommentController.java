package com.haiphung.comic_web.controller;

import com.haiphung.comic_web.entity.Comment;
import com.haiphung.comic_web.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/comments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCommentController {

    private final CommentRepository commentRepository;

    @GetMapping
    public String listComments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            Model model) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Comment> comments;
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            comments = commentRepository.findByContentContainingIgnoreCase(keyword.trim(), pageable);
        } else {
            comments = commentRepository.findAll(pageable);
        }
        
        model.addAttribute("comments", comments.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", comments.getTotalPages());
        model.addAttribute("totalElements", comments.getTotalElements());
        model.addAttribute("keyword", keyword);
        
        return "admin-comments";
    }

    @PostMapping("/delete/{id}")
    public String deleteComment(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        if (commentRepository.existsById(id)) {
            commentRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Xóa bình luận thành công!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy bình luận");
        }
        return "redirect:/admin/comments";
    }
}
