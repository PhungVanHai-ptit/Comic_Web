package com.haiphung.comic_web.service;

import com.haiphung.comic_web.entity.Chapter;
import com.haiphung.comic_web.entity.Comic;
import com.haiphung.comic_web.entity.User;
import com.haiphung.comic_web.repository.ChapterRepository;
import com.haiphung.comic_web.repository.ComicRepository;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;

import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class ChapterService {

    private final ChapterRepository chapterRepository;
    private final ComicRepository comicRepository;
    private final MinioService minioService;

    public List<Chapter> getChaptersByComic(Comic comic) {
        return chapterRepository.findByComicOrderByChapterNumDesc(comic);
    }

    public Page<Chapter> getChaptersByComicPaged(Comic comic, Pageable pageable) {
        return chapterRepository.findByComic(comic, pageable);
    }

    public List<Chapter> searchChaptersByNumber(Comic comic, BigDecimal num) {
        if (num == null) return getChaptersByComic(comic);
        
        // Neu num la so nguyen N, tim trong khoang [N, N+1)
        // Neu num co phan thap phan, tim dung so do (hoac trong khoang nho hon?)
        // De don gian theo yeu cau: "nhập 1 có thể ra từ 1 - 1,9"
        if (num.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal end = num.add(BigDecimal.ONE);
            return chapterRepository.findByComicAndChapterNumBetweenOrderByChapterNumDesc(comic, num, end.subtract(new BigDecimal("0.01")));
        } else {
            // Neu nhap so le nhu 1.5, co the user muon tim dung 1.5 hoac cac phien ban 1.5x
            // Tam thoi de dung so do
            return chapterRepository.findByComicAndChapterNumBetweenOrderByChapterNumDesc(comic, num, num);
        }
    }

    public Optional<Chapter> getChapterById(Integer id) {
        return chapterRepository.findById(id);
    }

    @Transactional
    public Chapter saveChapter(Chapter chapter) {
        Chapter saved = chapterRepository.save(chapter);
        refreshLastChapterAt(saved.getComic());
        return saved;
    }

    @Transactional
    public void deleteChapter(Integer id) {
        chapterRepository.findById(id).ifPresent(chapter -> {
            Comic comic = chapter.getComic();
//            if (chapter.getImagePaths() != null && !chapter.getImagePaths().isEmpty()) {
                for (String path : chapter.getImagePaths()) {
                    try { minioService.deleteFileByPath(path); } catch (Exception e) {}
                }
//            } else if (chapter.getResourcePath() != null && !chapter.getResourcePath().isEmpty()) {
//                // Fallback for un-migrated old data
//                String folderPath = "comic-" + comic.getComicId() + "/chapters/" + chapter.getResourcePath() + "/";
//                minioService.deleteFolderRecursive(folderPath);
//            }
            chapterRepository.delete(chapter);
            refreshLastChapterAt(comic);
        });
    }

    private void refreshLastChapterAt(Comic comic) {
        Optional<Chapter> latestOpt = chapterRepository.findTopByComicOrderByCreatedAtDesc(comic);
        if (latestOpt.isPresent()) {
            comic.setLastChapterAt(latestOpt.get().getCreatedAt());
        } else {
            comic.setLastChapterAt(comic.getCreatedAt());
        }
        comicRepository.save(comic);
    }

    public Optional<Comic> getComicById(Integer comicId) {
        return comicRepository.findById(comicId);
    }

    // Safe JSON Upload Logic
    @Transactional(rollbackFor = Exception.class)
    public void processChapterImages(Chapter chapter, MultipartFile[] files) throws Exception {
        if (files == null || files.length == 0 || files[0].isEmpty()) return;

        Integer comicId = chapter.getComic().getComicId();
        Integer chapterId = chapter.getChapterId();
        String comicFolder = "comic-" + comicId;
        String chapterFolder = "chapter-" + chapterId;
        
        List<String> uploadedPaths = new ArrayList<>();
        
        try {
            int count = 0;
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    count++;
                    String ext = minioService.getFileExtension(file.getOriginalFilename());
                    String customName = count + "-" + System.currentTimeMillis() + ext;
                    String objectName = minioService.uploadChapterImageWithCustomName(comicFolder, chapterFolder, customName, file);
                    uploadedPaths.add(objectName);
                }
            }

            // Ghi mảng JSON vào DB
            chapter.setImagePaths(uploadedPaths);
            chapterRepository.save(chapter);
            refreshLastChapterAt(chapter.getComic());

        } catch (Exception e) {
            // DB fail hoặc upload fail -> Rollback an toàn
            for (String path : uploadedPaths) {
                try { minioService.deleteFileByPath(path); } catch (Exception ex) {}
            }
            throw new Exception("Lỗi khi tải ảnh lên, đã dọn dẹp file an toàn: " + e.getMessage());
        }
    }

    /**
     * Update chapter images logic 3-step Anti-Data-Loss Action
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateChapterImagesWithOrder(Chapter chapter, String unifiedOrder, MultipartFile[] newFiles) throws Exception {
        Integer comicId = chapter.getComic().getComicId();
        Integer chapterId = chapter.getChapterId();
        String comicFolder = "comic-" + comicId;
        String chapterFolder = "chapter-" + chapterId;

        List<String> oldImagePaths = chapter.getImagePaths() != null ? new ArrayList<>(chapter.getImagePaths()) : new ArrayList<>();
        List<String> newUploadedPaths = new ArrayList<>();
        List<String> finalImagePaths = new ArrayList<>();

        try {
            int slot = 0;

            if (unifiedOrder != null && !unifiedOrder.trim().isEmpty()) {
                String[] parts = unifiedOrder.split(",");
                for (String part : parts) {
                    part = part.trim();
                    if (part.isEmpty()) continue;

                    slot++;
                    if (part.startsWith("existing:")) {
                        int origIndex = Integer.parseInt(part.substring(9));
                        if(origIndex >= 0 && origIndex < oldImagePaths.size()) {
                            finalImagePaths.add(oldImagePaths.get(origIndex));
                        }
                    } else if (part.startsWith("new:")) {
                        int newIdx = Integer.parseInt(part.substring(4));
                        if (newFiles != null && newIdx < newFiles.length) {
                            MultipartFile file = newFiles[newIdx];
                            String ext = minioService.getFileExtension(file.getOriginalFilename());
                            String customName = slot + "-" + System.currentTimeMillis() + ext;
                            String objectName = minioService.uploadChapterImageWithCustomName(comicFolder, chapterFolder, customName, file);
                            newUploadedPaths.add(objectName);
                            finalImagePaths.add(objectName);
                        }
                    }
                }
            }

            if (finalImagePaths.isEmpty()) {
                throw new Exception("Chapter phải có ít nhất 1 ảnh!");
            }

            // Bước 2: Commit DB
            chapter.setImagePaths(finalImagePaths);
            chapterRepository.save(chapter);
            refreshLastChapterAt(chapter.getComic());

            // Bước 3: Dọn Rác  (Xóa file không cần thiết trên MinIO)
            List<String> orphanedPaths = new ArrayList<>(oldImagePaths);
            orphanedPaths.removeAll(finalImagePaths);
            
            for (String path : orphanedPaths) {
                try {
                    minioService.deleteFileByPath(path);
                } catch (Exception e) {
                    System.err.println("[CLEANUP NEEDED] Orphan file not deleted: " + path + " - Lỗi: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            // Bước Khẩn Cấp: Rollback chỉ ảnh MỚI upload nếu DB sập. Không chạm vào Dữ liệu đang có của Truyện.
            for (String path : newUploadedPaths) {
                try { minioService.deleteFileByPath(path); } catch (Exception ex) {}
            }
            throw new Exception("Lỗi cập nhật ảnh chapter: " + e.getMessage());
        }
    }

    @Transactional
    public Chapter prepareChapterReading(Integer chapterId, HttpSession session) {
        Optional<Chapter> chapterOpt = chapterRepository.findById(chapterId);
        if (!chapterOpt.isPresent()) return null;
        
        Chapter chapter = chapterOpt.get();
        
        // 1. Session-based view cooldown (1 hour)
        String sessionKey = "viewed_ch_" + chapterId;
        Long lastViewed = (Long) session.getAttribute(sessionKey);
        long now = System.currentTimeMillis();
        
        if (lastViewed == null || (now - lastViewed) > 3600_000) {
            // Increment Chapter views
            chapter.setViews((chapter.getViews() == null ? 0 : chapter.getViews()) + 1);
            chapterRepository.save(chapter);
            
            // Increment Comic total views
            Comic comic = chapter.getComic();
            comic.setTotalViews((comic.getTotalViews() == null ? 0 : comic.getTotalViews()) + 1);
            comicRepository.save(comic);
            
            // Update session
            session.setAttribute(sessionKey, now);
        }
        
        return chapter;
    }

    public List<Chapter> getChaptersForReading(Comic comic) {
        List<Chapter> chapters = new ArrayList<>(comic.getChapters());
        // chapters list is already sorted DESC if we added @OrderBy in Comic entity
        // But for reading navigation (Prev/Next), it's easier if it's ASC
        // actually let's just reverse it or sort it here to be safe
        chapters.sort(java.util.Comparator.comparing(Chapter::getChapterNum));
        return chapters;
    }

    //Kiểm tra user có quyền đọc chapter hay không.
    public boolean canUserReadChapter(Chapter chapter, User currentUser) {
        if (!chapter.isRequireLogin()) return true;
        return currentUser != null;
    }
}

