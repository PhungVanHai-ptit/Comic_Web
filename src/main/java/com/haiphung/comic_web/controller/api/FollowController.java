package com.haiphung.comic_web.controller.api;

import com.haiphung.comic_web.config.CustomUserDetails;
import com.haiphung.comic_web.entity.User;
import com.haiphung.comic_web.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/follow")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping("/comic/{comicId}")
    public ResponseEntity<Map<String, Object>> toggleFollow(@PathVariable("comicId") Integer comicId,
                                                               @AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = (userDetails != null) ? userDetails.getUser() : null;
        Map<String, Object> response = new HashMap<>();

        if (user == null) {
            response.put("success", false);
            response.put("message", "Vui lòng đăng nhập để theo dõi truyện");
            return ResponseEntity.status(401).body(response);
        }

        Map<String, Object> result = followService.toggleFollow(comicId, user);
        return ResponseEntity.ok(result);
    }
}