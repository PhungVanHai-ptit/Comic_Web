// package com.haiphung.comic_web.controller.api;

// import com.haiphung.comic_web.entity.Chapter;
// import com.haiphung.comic_web.repository.ChapterRepository;
// import lombok.RequiredArgsConstructor;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.access.prepost.PreAuthorize;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;

// import java.util.ArrayList;
// import java.util.List;

// @RestController
// @RequestMapping("/api/admin/system")
// @RequiredArgsConstructor
// @PreAuthorize("hasRole('ADMIN')")
// public class AdminMigrationController {

// private final ChapterRepository chapterRepository;

// @GetMapping("/migrate/chapters-json")
// public ResponseEntity<?> migrateChaptersToJson() {
// List<Chapter> chapters = chapterRepository.findAll();
// int migratedCount = 0;

// for (Chapter chapter : chapters) {
// // Chỉ migrate những chapter chưa có JSON paths và có data cũ hợp lệ
// if ((chapter.getImagePaths() == null || chapter.getImagePaths().isEmpty())
// && chapter.getImageCount() != null && chapter.getImageCount() > 0
// && chapter.getResourcePath() != null && !chapter.getResourcePath().isEmpty())
// {

// List<String> paths = new ArrayList<>();
// Integer comicId = chapter.getComic().getComicId();
// String resourcePath = chapter.getResourcePath();

// // Cấu trúc cũ: comic-{comicId}/chapters/{resourcePath}/{i}.jpg
// for (int i = 1; i <= chapter.getImageCount(); i++) {
// paths.add("comic-" + comicId + "/chapters/" + resourcePath + "/" + i +
// ".jpg");
// }

// chapter.setImagePaths(paths);
// migratedCount++;
// }
// }

// if (migratedCount > 0) {
// chapterRepository.saveAll(chapters);
// }

// return ResponseEntity.ok("MIGRATION_SUCCESS - Đã cập nhật thành không mảng
// list JSON cho các Chapters hiện tại. Tổng số lượng: " + migratedCount);
// }
// }
