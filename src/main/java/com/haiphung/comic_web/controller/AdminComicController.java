package com.haiphung.comic_web.controller;

import com.haiphung.comic_web.entity.Chapter;
import com.haiphung.comic_web.entity.Comic;
import com.haiphung.comic_web.entity.Genre;
import com.haiphung.comic_web.service.ComicService;
import com.haiphung.comic_web.service.ChapterService;
import com.haiphung.comic_web.service.GenreService;
import com.haiphung.comic_web.service.MinioService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/admin/comics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminComicController {

    private final ComicService comicService;
    private final GenreService genreService;
    private final ChapterService chapterService;
    private final MinioService minioService;

    @Value("${minio.url}")
    private String minioUrl;

    @GetMapping
    public String listComics(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer genreId,
            @RequestParam(required = false, defaultValue = "updatedAt") String sortBy,
            Model model) {
        
        Sort sort = Sort.by(Sort.Direction.DESC, "updatedAt");
        if ("title".equals(sortBy)) sort = Sort.by(Sort.Direction.ASC, "title");
        else if ("createdAt".equals(sortBy)) sort = Sort.by(Sort.Direction.DESC, "createdAt");
        else if ("updatedAt".equals(sortBy)) sort = Sort.by(Sort.Direction.DESC, "updatedAt");
        
        Page<Comic> comics = comicService.searchAdvanced(keyword, status, genreId, PageRequest.of(page, size, sort));
        
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("genreId", genreId);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("comics", comics);
        model.addAttribute("genres", genreService.getAllGenresSorted());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", comics.getTotalPages());
        model.addAttribute("totalElements", comics.getTotalElements());
        model.addAttribute("minioUrl", minioUrl);
        return "admin-comics";
    }

    @GetMapping("/add")
    public String showAddComicForm(Model model) {
        model.addAttribute("comic", new Comic());
        List<Genre> genres = genreService.getAllGenresSorted();
        model.addAttribute("genres", genres);
        return "admin-add-comic";
    }

    @PostMapping("/add")
    public String addComic(
            @ModelAttribute Comic comic,
            @RequestParam(required = false) List<Integer> genreIds,
            @RequestParam("coverFile") MultipartFile coverFile,
            RedirectAttributes redirectAttributes) {

        if (genreIds != null && !genreIds.isEmpty()) {
            comic.setGenres(genreService.getGenresByIds(genreIds));
        }

        // Set lastChapterAt to current time if new
        if (comic.getLastChapterAt() == null) {
            comic.setLastChapterAt(java.time.LocalDateTime.now());
        }

        // Save first to generate ID
        Comic savedComic = comicService.saveComic(comic);


        if (coverFile != null && !coverFile.isEmpty()) {
            try {
                String objectName = minioService.uploadComicCover(savedComic.getComicId(), coverFile);
                savedComic.setCoverImage(objectName);
                comicService.saveComic(savedComic);
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Lỗi upload ảnh bìa: " + e.getMessage());
                return "redirect:/admin/comics";
            }
        }

        redirectAttributes.addFlashAttribute("success", "Thêm truyện thành công!");
        return "redirect:/admin/comics";
    }

    @GetMapping("/edit/{id}")
    public String showEditComicForm(
            @PathVariable Integer id,
            @RequestParam(required = false) BigDecimal chapterSearch,
            @RequestParam(defaultValue = "0") int chapterPage,
            Model model,
            RedirectAttributes redirectAttributes) {
        return comicService.getComicById(id)
                .map(comic -> {
                    model.addAttribute("comic", comic);
                    
                    Page<Chapter> chaptersPage;
                    if (chapterSearch != null) {
                        // Khi tìm kiếm, không phân trang
                        List<Chapter> chapters = chapterService.searchChaptersByNumber(comic, chapterSearch);
                        chaptersPage = new org.springframework.data.domain.PageImpl<>(chapters);
                        model.addAttribute("chapterSearch", chapterSearch);
                    } else {
                        Pageable pageable = PageRequest.of(chapterPage, 10, Sort.by(Sort.Direction.DESC, "chapterNum"));
                        chaptersPage = chapterService.getChaptersByComicPaged(comic, pageable);
                    }
                    
                    model.addAttribute("chapters", chaptersPage.getContent());
                    model.addAttribute("chapterPage", chapterPage);
                    model.addAttribute("totalChapterPages", chaptersPage.getTotalPages());
                    model.addAttribute("totalChapters", chaptersPage.getTotalElements());
                    
                    List<Genre> genres = genreService.getAllGenresSorted();
                    model.addAttribute("genres", genres);
                    model.addAttribute("minioUrl", minioUrl);
                    return "admin-edit-comic";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Không tìm thấy truyện");
                    return "redirect:/admin/comics";
                });
    }

    @PostMapping("/edit/{id}")
    public String editComic(
            @PathVariable Integer id,
            @ModelAttribute Comic comic,
            @RequestParam(required = false) List<Integer> genreIds,
            @RequestParam(value = "coverFile", required = false) MultipartFile coverFile,
            RedirectAttributes redirectAttributes) {
            
        Comic existingComic = comicService.getComicById(id).orElse(null);
        if (existingComic == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy truyện");
            return "redirect:/admin/comics";
        }

        existingComic.setTitle(comic.getTitle());
        existingComic.setAuthor(comic.getAuthor());
        existingComic.setDescription(comic.getDescription());
        existingComic.setStatus(comic.getStatus());

        if (genreIds != null) {
            existingComic.setGenres(genreService.getGenresByIds(genreIds));
        } else {
            existingComic.setGenres(null);
        }

        if (coverFile != null && !coverFile.isEmpty()) {
            try {
                if (existingComic.getCoverImage() != null) {
                    minioService.deleteComicCover(existingComic.getCoverImage());
                }
                String newCover = minioService.uploadComicCover(existingComic.getComicId(), coverFile);
                existingComic.setCoverImage(newCover);
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Lỗi cập nhật ảnh bìa: " + e.getMessage());
                return "redirect:/admin/comics";
            }
        }

        comicService.saveComic(existingComic);
        redirectAttributes.addFlashAttribute("success", "Cập nhật truyện thành công!");
        return "redirect:/admin/comics";
    }

    @PostMapping("/delete/{id}")
    public String deleteComic(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        Comic comic = comicService.getComicById(id).orElse(null);
        if (comic == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy truyện");
            return "redirect:/admin/comics";
        }
        try {
            if (comic.getCoverImage() != null) {
                minioService.deleteComicCover(comic.getCoverImage());
            }
        } catch (Exception e) {
            System.err.println("Failed to delete cover: " + e.getMessage());
        }
        
        comicService.deleteComic(id);
        redirectAttributes.addFlashAttribute("success", "Xóa truyện thành công!");
        return "redirect:/admin/comics";
    }

}
