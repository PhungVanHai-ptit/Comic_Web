package com.haiphung.comic_web.controller.api;

import com.haiphung.comic_web.config.CustomUserDetails;
import com.haiphung.comic_web.entity.User;
import com.haiphung.comic_web.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/comic/{comicId}")
    public ResponseEntity<Map<String, Object>> addComment(@PathVariable("comicId") Integer comicId,
                                                          @RequestBody Map<String, String> requestBody,
                                                          @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = (userDetails != null) ? userDetails.getUser() : null;
        Map<String, Object> response = new HashMap<>();

        if (user == null) {
            response.put("success", false);
            response.put("message", "Vui lòng đăng nhập để bình luận");
            return ResponseEntity.status(401).body(response);
        }

        String content = requestBody.get("content");
        Map<String, Object> result = commentService.addComment(comicId, content, user);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Map<String, Object>> deleteComment(@PathVariable("commentId") Integer commentId,
                                                               @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = (userDetails != null) ? userDetails.getUser() : null;
        Map<String, Object> response = new HashMap<>();

        if (user == null) {
            response.put("success", false);
            response.put("message", "Vui lòng đăng nhập");
            return ResponseEntity.status(401).body(response);
        }

        Map<String, Object> result = commentService.deleteComment(commentId, user);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/comic/{comicId}")
    public ResponseEntity<Map<String, Object>> getComments(@PathVariable("comicId") Integer comicId,
                                                            @RequestParam(value = "page", defaultValue = "0") int page) {
        Map<String, Object> result = commentService.getComments(comicId, page);
        return ResponseEntity.ok(result);
    }
}