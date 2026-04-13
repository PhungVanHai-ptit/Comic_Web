package com.haiphung.comic_web.controller;

import com.haiphung.comic_web.entity.Chapter;
import com.haiphung.comic_web.entity.Comic;
import com.haiphung.comic_web.service.ChapterService;
import com.haiphung.comic_web.service.ComicService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/admin/comics/{comicId}/chapters")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminChapterController {

    private final ChapterService adminChapterService;
    private final ComicService comicService;

    @Value("${minio.url}")
    private String minioUrl;

    @GetMapping("/add")
    public String showAddChapterForm(@PathVariable Integer comicId, Model model, RedirectAttributes redirectAttributes) {
        try {
            Optional<Comic> comicOpt = adminChapterService.getComicById(comicId);
            if (comicOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy truyện!");
                return "redirect:/admin/comics";
            }
            
            Chapter chapter = new Chapter();
            chapter.setComic(comicOpt.get());
            
            model.addAttribute("comic", comicOpt.get());
            model.addAttribute("chapter", chapter);
            return "admin-add-chap";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "[GET /add] " + e.getClass().getName() + ": " + e.getMessage());
            return "redirect:/admin/comics";
        }
    }

    @PostMapping("/add")
    public String addChapter(
            @PathVariable Integer comicId,
            @ModelAttribute Chapter chapter,
            @RequestParam("images") MultipartFile[] files,
            RedirectAttributes redirectAttributes) {
            
        try {
            Optional<Comic> comicOpt = adminChapterService.getComicById(comicId);
            if (comicOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy truyện!");
                return "redirect:/admin/comics";
            }
            
            chapter.setComic(comicOpt.get());
            Chapter savedChapter = adminChapterService.saveChapter(chapter);

            if (files != null && files.length > 0 && !files[0].isEmpty()) {
                try {
                    adminChapterService.processChapterImages(savedChapter, files);
                    redirectAttributes.addFlashAttribute("success", "Thêm chapter thành công!");
                } catch (Exception e) {
                    // Rollback database record safely if file upload logic failed midway
                    adminChapterService.deleteChapter(savedChapter.getChapterId());
                    redirectAttributes.addFlashAttribute("error", e.getMessage());
                    return "redirect:/admin/comics/" + comicId + "/chapters/add";
                }
            } else {
                 redirectAttributes.addFlashAttribute("success", "Thêm chapter thành công (chưa có ảnh)!");
            }

            return "redirect:/admin/comics/edit/" + comicId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Server Error: " + e.getClass().getName() + " - " + e.getMessage());
            return "redirect:/admin/comics/" + comicId + "/chapters/add";
        } finally {
            comicService.updateLastChapterAt(comicId);
        }

    }

    @GetMapping("/edit/{chapterId}")
    public String showEditChapterForm(@PathVariable Integer comicId, @PathVariable Integer chapterId, Model model, RedirectAttributes redirectAttributes) {
        try {
            Optional<Chapter> chapterOpt = adminChapterService.getChapterById(chapterId);
            if (chapterOpt.isEmpty() || !chapterOpt.get().getComic().getComicId().equals(comicId)) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy chapter!");
                return "redirect:/admin/comics/edit/" + comicId;
            }

            model.addAttribute("comic", chapterOpt.get().getComic());
            model.addAttribute("chapter", chapterOpt.get());
            model.addAttribute("minioUrl", minioUrl);
            return "admin-edit-chap";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "[GET /edit] " + e.getClass().getName() + ": " + e.getMessage());
            return "redirect:/admin/comics/edit/" + comicId;
        }
    }

    @PostMapping("/edit/{chapterId}")
    public String editChapter(
            @PathVariable Integer comicId,
            @PathVariable Integer chapterId,
            @ModelAttribute Chapter chapter,
            @RequestParam(value = "unifiedOrder", required = false) String unifiedOrder,
            @RequestParam(value = "images", required = false) MultipartFile[] files,
            RedirectAttributes redirectAttributes) {
            
        Optional<Chapter> existingOpt = adminChapterService.getChapterById(chapterId);
        if (existingOpt.isEmpty() || !existingOpt.get().getComic().getComicId().equals(comicId)) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy chapter!");
            return "redirect:/admin/comics/edit/" + comicId;
        }
        
        Chapter existing = existingOpt.get();
        existing.setChapterNum(chapter.getChapterNum());
        existing.setTitle(chapter.getTitle());
        existing.setRequireLogin(chapter.isRequireLogin());
        
        Chapter savedChapter = adminChapterService.saveChapter(existing);

        // Check if user made any image changes
        boolean hasNewFiles = files != null && files.length > 0 && !files[0].isEmpty();
        boolean hasChanges = (unifiedOrder != null && !unifiedOrder.trim().isEmpty());
        
        if (hasChanges) {
            try {
                adminChapterService.updateChapterImagesWithOrder(savedChapter, unifiedOrder, hasNewFiles ? files : new MultipartFile[0]);
                redirectAttributes.addFlashAttribute("success", "Cập nhật chapter và ảnh thành công!");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", e.getMessage());
                return "redirect:/admin/comics/" + comicId + "/chapters/edit/" + chapterId;
            }
        } else {
            redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin chapter thành công!");
        }

        comicService.updateLastChapterAt(comicId);
        return "redirect:/admin/comics/edit/" + comicId;

    }

    @PostMapping("/delete/{chapterId}")
    public String deleteChapter(
            @PathVariable Integer comicId, 
            @PathVariable Integer chapterId,
            RedirectAttributes redirectAttributes) {
        
        Optional<Chapter> chapterOpt = adminChapterService.getChapterById(chapterId);
        if (chapterOpt.isPresent() && chapterOpt.get().getComic().getComicId().equals(comicId)) {
            adminChapterService.deleteChapter(chapterId);
            redirectAttributes.addFlashAttribute("success", "Đã xóa chapter thành công!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy chapter để xóa!");
        }
        
        comicService.updateLastChapterAt(comicId);
        return "redirect:/admin/comics/edit/" + comicId;

    }
}
