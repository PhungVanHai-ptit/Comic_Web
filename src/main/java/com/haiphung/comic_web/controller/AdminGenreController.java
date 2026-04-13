package com.haiphung.comic_web.controller;

import com.haiphung.comic_web.entity.Genre;
import com.haiphung.comic_web.service.GenreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/genres")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminGenreController {

    private final GenreService adminGenreService;
    private static final Pageable DEFAULT_GENRE_PAGEABLE =
            PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "name"));

    @GetMapping
    public String listGenres(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            Model model) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        Page<Genre> genres = adminGenreService.searchGenresByName(keyword, pageable);
        model.addAttribute("keyword", keyword);
        model.addAttribute("genre", new Genre());
        model.addAttribute("genres", genres);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", genres.getTotalPages());
        model.addAttribute("totalElements", genres.getTotalElements());
        return "admin-add-genre";
    }

    @GetMapping("/add")
    public String showAddGenreForm(Model model) {
        model.addAttribute("genre", new Genre());
        Page<Genre> genres = adminGenreService.getAllGenres(DEFAULT_GENRE_PAGEABLE);
        model.addAttribute("genres", genres);
        model.addAttribute("currentPage", 0);
        model.addAttribute("totalPages", genres.getTotalPages());
        model.addAttribute("totalElements", genres.getTotalElements());
        return "admin-add-genre";
    }

    @PostMapping("/add")
    public String addGenre(
            @Valid @ModelAttribute Genre genre,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) {
        if (adminGenreService.existsByName(genre.getName())) {
            result.rejectValue("name", "error.genre", "Tên thể loại đã tồn tại");
        }

        if (result.hasErrors()) {
            Page<Genre> genres = adminGenreService.getAllGenres(DEFAULT_GENRE_PAGEABLE);
            model.addAttribute("genres", genres);
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", genres.getTotalPages());
            model.addAttribute("totalElements", genres.getTotalElements());
            return "admin-add-genre";
        }

        adminGenreService.saveGenre(genre);
        redirectAttributes.addFlashAttribute("success", "Thêm thể loại thành công!");
        return "redirect:/admin/genres";
    }

    @GetMapping("/edit/{id}")
    public String showEditGenreForm(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        return adminGenreService.getGenreById(id)
                .map(genre -> {
                    model.addAttribute("genre", genre);
                    Page<Genre> genres = adminGenreService.getAllGenres(DEFAULT_GENRE_PAGEABLE);
                    model.addAttribute("genres", genres);
                    model.addAttribute("currentPage", 0);
                    model.addAttribute("totalPages", genres.getTotalPages());
                    model.addAttribute("totalElements", genres.getTotalElements());
                    return "admin-edit-genre";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "Không tìm thấy thể loại");
                    return "redirect:/admin/genres";
                });
    }

    @PostMapping("/edit/{id}")
    public String editGenre(
            @PathVariable Integer id,
            @Valid @ModelAttribute Genre genre,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (adminGenreService.existsByNameAndIdNot(genre.getName(), id)) {
            result.rejectValue("name", "error.genre", "Tên thể loại đã tồn tại");
        }

        if (result.hasErrors()) {
            Page<Genre> genres = adminGenreService.getAllGenres(DEFAULT_GENRE_PAGEABLE);
            model.addAttribute("genres", genres);
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", genres.getTotalPages());
            model.addAttribute("totalElements", genres.getTotalElements());
            return "admin-edit-genre";
        }

        genre.setGenreId(id);
        adminGenreService.saveGenre(genre);
        redirectAttributes.addFlashAttribute("success", "Cập nhật thể loại thành công!");
        return "redirect:/admin/genres";
    }

    @PostMapping("/delete/{id}")
    public String deleteGenre(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        if (adminGenreService.getGenreById(id).isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy thể loại");
            return "redirect:/admin/genres";
        }
        adminGenreService.deleteGenre(id);
        redirectAttributes.addFlashAttribute("success", "Xóa thể loại thành công!");
        return "redirect:/admin/genres";
    }
}