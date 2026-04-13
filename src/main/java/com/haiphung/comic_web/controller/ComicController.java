package com.haiphung.comic_web.controller;

import com.haiphung.comic_web.config.CustomUserDetails;
import com.haiphung.comic_web.entity.*;
import com.haiphung.comic_web.repository.CommentRepository;
import com.haiphung.comic_web.repository.FollowRepository;
import com.haiphung.comic_web.service.ChapterService;
import com.haiphung.comic_web.service.ComicService;
import com.haiphung.comic_web.service.GenreService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpSession;


import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class ComicController {

    private final ComicService comicService;
    private final GenreService genreService;
    private final ChapterService chapterService;
    private final FollowRepository followRepository;
    private final CommentRepository commentRepository;

    @Value("${minio.url}")
    private String minioUrl;

    /**
     * Trang chủ - hiển thị top 5 truyện lượt xem cao nhất (banner)
     * và 12 truyện mới cập nhật nhất (lưới)
     */
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("topComics", comicService.getTop5ByViews());
        model.addAttribute("latestComics", comicService.getLatestComics());
        model.addAttribute("allGenres", genreService.getAllGenresSorted());
        model.addAttribute("minioUrl", minioUrl);
        return "home";
    }

    @GetMapping("/all-comic")
    public String allComic(@RequestParam(value = "page", defaultValue = "0") int page,
                           @RequestParam(value = "sort", defaultValue = "latest") String sort,
                           @RequestParam(value = "status", required = false) String status,
                           Model model) {
        Sort sortOrder;
        switch (sort) {
            case "views":
                sortOrder = Sort.by(Sort.Direction.DESC, "totalViews");
                break;
            case "az":
                sortOrder = Sort.by(Sort.Direction.ASC, "title");
                break;
            case "za":
                sortOrder = Sort.by(Sort.Direction.DESC, "title");
                break;
            case "latest":
            default:
                sortOrder = Sort.by(Sort.Direction.DESC, "lastChapterAt");
                break;
        }

        int pageSize = 12;
        Pageable pageable = PageRequest.of(page, pageSize, sortOrder);
        Page<Comic> comics = comicService.searchAdvanced(null, status, null, pageable);

        // DEBUG LOG - Xóa sau khi fix xong
        System.out.println("========== DEBUG INFO ==========");
        System.out.println("Requested Page: " + page);
        System.out.println("Page Size: " + pageSize);
        System.out.println("Total Pages: " + comics.getTotalPages());
        System.out.println("Total Elements: " + comics.getTotalElements());
        System.out.println("Current Page Content Size: " + comics.getContent().size());
        System.out.println("Sort: " + sort);
        System.out.println("Status: " + status);
        System.out.println("================================");

        model.addAttribute("comics", comics.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", comics.getTotalPages());
        model.addAttribute("totalElements", comics.getTotalElements());
        model.addAttribute("pageSize", pageSize);  // ← THÊM DÒNG NÀY
        model.addAttribute("currentSort", sort);
        model.addAttribute("currentStatus", status);
        model.addAttribute("minioUrl", minioUrl);
        model.addAttribute("allGenres", genreService.getAllGenresSorted());

        return "all-comic";
    }

    @GetMapping("/comic-detail/{id}")
    public String comicDetail(@PathVariable("id") Integer id,
                               @RequestParam(value = "commentPage", defaultValue = "0") int commentPage,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               Model model) {
        User currentUser = (userDetails != null) ? userDetails.getUser() : null;
        Optional<Comic> comicOpt = comicService.getComicById(id);
        if (!comicOpt.isPresent()) {
            return "redirect:/all-comic";
        }

        Comic comic = comicOpt.get();
        List<Chapter> chapters = chapterService.getChaptersByComic(comic);

        Pageable commentPageable = PageRequest.of(commentPage, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Comment> commentsPage = commentRepository.findByComicOrderByCreatedAtDesc(comic, commentPageable);

        boolean isFollowing = false;
        if (currentUser != null) {
            isFollowing = followRepository.existsByUserAndComic(currentUser, comic);
        }

        model.addAttribute("comic", comic);
        model.addAttribute("chapters", chapters);
        model.addAttribute("comments", commentsPage.getContent());
        model.addAttribute("commentPage", commentPage);
        model.addAttribute("totalCommentPages", commentsPage.getTotalPages());
        model.addAttribute("totalComments", commentsPage.getTotalElements());
        model.addAttribute("isFollowing", isFollowing);
        model.addAttribute("minioUrl", minioUrl);
        model.addAttribute("allGenres", genreService.getAllGenresSorted());

        return "comic-detail";
    }

    @GetMapping("/following-comics")
    public String followingComics(@RequestParam(value = "page", defaultValue = "0") int page,
                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                  Model model) {
        User currentUser = (userDetails != null) ? userDetails.getUser() : null;
        if (currentUser == null) {
            return "redirect:/auth/login?required";
        }

        Pageable pageable = PageRequest.of(page, 24, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Comic> followingComics = followRepository.findComicsByUserId(currentUser.getUserId(), pageable);

        model.addAttribute("followingComics", followingComics.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", followingComics.getTotalPages());
        model.addAttribute("totalElements", followingComics.getTotalElements());
        model.addAttribute("minioUrl", minioUrl);
        model.addAttribute("allGenres", genreService.getAllGenresSorted());

        return "following-comics";
    }

    @GetMapping("/comics-by-genre/{id}")
    public String comicsByGenre(@PathVariable("id") Integer id,
                                @RequestParam(value = "page", defaultValue = "0") int page,
                                @RequestParam(value = "sort", defaultValue = "latest") String sort,
                                Model model) {
        
        Genre genre = genreService.getGenreById(id).orElse(null);
        if (genre == null) {
            return "redirect:/all-comic";
        }

        Sort sortOrder;
        switch (sort) {
            case "views":
                sortOrder = Sort.by(Sort.Direction.DESC, "totalViews");
                break;
            case "az":
                sortOrder = Sort.by(Sort.Direction.ASC, "title");
                break;
            case "za":
                sortOrder = Sort.by(Sort.Direction.DESC, "title");
                break;
            case "latest":
            default:
                sortOrder = Sort.by(Sort.Direction.DESC, "lastChapterAt");
                break;
        }

        Pageable pageable = PageRequest.of(page, 12, sortOrder);
        Page<Comic> comics = comicService.getComicsByGenre(id, pageable);

        model.addAttribute("genre", genre);
        model.addAttribute("comics", comics.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", comics.getTotalPages());
        model.addAttribute("totalElements", comics.getTotalElements());
        model.addAttribute("currentSort", sort);
        model.addAttribute("minioUrl", minioUrl);
        model.addAttribute("allGenres", genreService.getAllGenresSorted());

        return "comics-by-genre";
    }

    @GetMapping("/comic-reading/{chapterId}")
    public String comicReading(@PathVariable("chapterId") Integer chapterId,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               HttpSession session,
                               Model model) {
        Chapter chapter = chapterService.prepareChapterReading(chapterId, session);
        if (chapter == null) {
            return "redirect:/all-comic";
        }

        Comic comic = chapter.getComic();
        List<Chapter> chapters = chapterService.getChaptersForReading(comic);
        User currentUser = (userDetails != null) ? userDetails.getUser() : null;

        // Kiểm tra quyền đọc chapter
        if (!chapterService.canUserReadChapter(chapter, currentUser)) {
            return "redirect:/auth/login?required";
        }

        Chapter prevChapter = null;
        Chapter nextChapter = null;
        
        for (int i = 0; i < chapters.size(); i++) {
            if (chapters.get(i).getChapterId().equals(chapterId)) {
                if (i > 0) prevChapter = chapters.get(i - 1);
                if (i < chapters.size() - 1) nextChapter = chapters.get(i + 1);
                break;
            }
        }

        boolean isFollowing = false;
        if (currentUser != null) {
            isFollowing = followRepository.existsByUserAndComic(currentUser, comic);
        }

        model.addAttribute("comic", comic);
        model.addAttribute("chapter", chapter);
        model.addAttribute("chapters", chapters);
        model.addAttribute("prevChapter", prevChapter);
        model.addAttribute("nextChapter", nextChapter);
        model.addAttribute("isFollowing", isFollowing);
        model.addAttribute("minioUrl", minioUrl);
        model.addAttribute("allGenres", genreService.getAllGenresSorted());

        return "comic-reading";
    }

}

