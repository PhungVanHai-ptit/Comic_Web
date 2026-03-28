package com.haiphung.comic_web.controller;

import com.haiphung.comic_web.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/admin/comments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCommentController {

    private final CommentService commentService;

    @GetMapping
    public String listComments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            Model model) {
        
        Map<String, Object> result = commentService.getAllCommentsForAdmin(page, size, keyword);
        
        model.addAttribute("comments", result.get("comments"));
        model.addAttribute("currentPage", result.get("currentPage"));
        model.addAttribute("totalPages", result.get("totalPages"));
        model.addAttribute("totalElements", result.get("totalElements"));
        model.addAttribute("keyword", result.get("keyword"));
        
        return "admin-comments";
    }

    @PostMapping("/delete/{id}")
    public String deleteComment(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        Map<String, Object> result = commentService.deleteCommentAsAdmin(id);
        
        if ((Boolean) result.get("success")) {
            redirectAttributes.addFlashAttribute("success", result.get("message"));
        } else {
            redirectAttributes.addFlashAttribute("error", result.get("message"));
        }
        
        return "redirect:/admin/comments";
    }
}
