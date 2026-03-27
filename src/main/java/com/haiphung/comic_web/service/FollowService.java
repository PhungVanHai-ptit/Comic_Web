package com.haiphung.comic_web.service;

import com.haiphung.comic_web.entity.Comic;
import com.haiphung.comic_web.entity.Follow;
import com.haiphung.comic_web.entity.FollowId;
import com.haiphung.comic_web.entity.User;
import com.haiphung.comic_web.repository.ComicRepository;
import com.haiphung.comic_web.repository.FollowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final ComicRepository comicRepository;

    @Transactional
    public Map<String, Object> toggleFollow(Integer comicId, User user) {
        Map<String, Object> response = new HashMap<>();

        Comic comic = comicRepository.findById(comicId).orElse(null);
        if (comic == null) {
            response.put("success", false);
            response.put("message", "Truyện không tồn tại");
            return response;
        }

        boolean isFollowing = followRepository.existsByUserAndComic(user, comic);
        if (isFollowing) {
            followRepository.deleteByUserIdAndComicId(user.getUserId(), comicId);
            comic.setTotalFollows(Math.max(0, (comic.getTotalFollows() != null ? comic.getTotalFollows() : 0) - 1));
            comicRepository.save(comic);
            response.put("following", false);
            response.put("message", "Đã bỏ theo dõi");
        } else {
            Follow follow = new Follow();
            FollowId followId = new FollowId();
            followId.setUserId(user.getUserId());
            followId.setComicId(comicId);
            follow.setId(followId);
            follow.setUser(user);
            follow.setComic(comic);
            followRepository.save(follow);
            comic.setTotalFollows((comic.getTotalFollows() != null ? comic.getTotalFollows() : 0) + 1);
            comicRepository.save(comic);
            response.put("following", true);
            response.put("message", "Đã theo dõi");
        }

        response.put("success", true);
        response.put("followCount", comic.getTotalFollows());
        return response;
    }

    public boolean isFollowing(Integer comicId, User user) {
        if (user == null) return false;
        Comic comic = comicRepository.findById(comicId).orElse(null);
        if (comic == null) return false;
        return followRepository.existsByUserAndComic(user, comic);
    }
}