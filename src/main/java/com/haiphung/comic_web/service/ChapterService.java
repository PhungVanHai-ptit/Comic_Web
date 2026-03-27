package com.haiphung.comic_web.service;

import com.haiphung.comic_web.entity.Chapter;
import com.haiphung.comic_web.entity.Comic;
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
            if (chapter.getResourcePath() != null) {
                // Xóa ảnh trên MinIO
                String folderPath = "comic-" + comic.getComicId() + "/chapters/" + chapter.getResourcePath() + "/";
                minioService.deleteFolderRecursive(folderPath);
            }
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

    // Atomic Temp Upload Commit Logic
    @Transactional(rollbackFor = Exception.class)
    public void processChapterImages(Chapter chapter, MultipartFile[] files) throws Exception {
        if (files == null || files.length == 0 || files[0].isEmpty()) return;

        Integer comicId = chapter.getComic().getComicId();
        Integer chapterId = chapter.getChapterId();
        String comicFolder = "comic-" + comicId;
        String tempFolder = "upload-" + chapterId;
        
        // 1. Upload vào TEMP
        try {
            int count = 0;
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    count++;
                    minioService.uploadTempChapterImage(comicFolder, tempFolder, file, count);
                }
            }

            // 2. TẤT CẢ SUCCESS -> Commit
            String newMilisec = System.currentTimeMillis() + "";
            String newFinalFolder = "chapter-" + chapterId + "-" + newMilisec;

            // Xóa folder chính cũ nếu tồn tại (Update chapter)
            if (chapter.getResourcePath() != null && !chapter.getResourcePath().isEmpty()) {
                minioService.deleteFolderRecursive(comicFolder + "/chapters/" + chapter.getResourcePath() + "/");
            }

            // Move (Copy & Delete) từ temp sang folder chính
            minioService.commitTempChapter(comicFolder, tempFolder, newFinalFolder, count);

            // Cập nhật CSDL
            chapter.setResourcePath(newFinalFolder);
            chapter.setImageCount(count);
            chapterRepository.save(chapter);
            refreshLastChapterAt(chapter.getComic());

        } catch (Exception e) {
            // 3. ANY ERROR -> Xóa folder TEMP
            minioService.deleteFolderRecursive(comicFolder + "/chapters/temp/" + tempFolder + "/");
            throw new Exception("Lỗi khi tải ảnh lên, đã xóa bộ nhớ đệm an toàn: " + e.getMessage());
        }
    }

    /**
     * Update chapter images, keeping existing images in a new user-specified order plus appending new ones.
     * @param chapter         The chapter entity (must have resourcePath set if it has images)
     * @param keptImageIndices Comma-separated 1-based indices of the old images to keep, in desired order (e.g. "3,1,2")
     * @param newFiles        New image files uploaded by the user (may be empty)
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateChapterImagesWithOrder(Chapter chapter, String keptImageIndices, MultipartFile[] newFiles) throws Exception {
        Integer comicId = chapter.getComic().getComicId();
        Integer chapterId = chapter.getChapterId();
        String comicFolder = "comic-" + comicId;
        String oldResourcePath = chapter.getResourcePath();
        String tempFolder = "upload-edit-" + chapterId;
        String tempPrefix = comicFolder + "/chapters/temp/" + tempFolder + "/";

        try {
            int slot = 0;

            // Step 1: Copy kept existing images to temp in specified order
            if (keptImageIndices != null && !keptImageIndices.trim().isEmpty() && oldResourcePath != null && !oldResourcePath.trim().isEmpty()) {
                String[] parts = keptImageIndices.split(",");
                for (String part : parts) {
                    part = part.trim();
                    if (part.isEmpty()) continue;
                    int origIndex = Integer.parseInt(part);
                    slot++;
                    String srcObj = comicFolder + "/chapters/" + oldResourcePath + "/" + origIndex + ".jpg";
                    String dstObj = tempPrefix + slot + ".jpg";
                    minioService.copyObject(srcObj, dstObj);
                }
            }

            // Step 2: Upload new files to temp after existing ones
            if (newFiles != null) {
                for (MultipartFile file : newFiles) {
                    if (file != null && !file.isEmpty()) {
                        slot++;
                        minioService.uploadTempChapterImage(comicFolder, tempFolder, file, slot);
                    }
                }
            }

            if (slot == 0) {
                throw new Exception("Chapter phải có ít nhất 1 ảnh!");
            }

            // Step 3: Commit - create new final folder from temp
            String newFinalFolder = "chapter-" + chapterId + "-" + System.currentTimeMillis();
            minioService.commitTempChapter(comicFolder, tempFolder, newFinalFolder, slot);

            // Step 4: Delete old folder
            if (oldResourcePath != null && !oldResourcePath.trim().isEmpty()) {
                minioService.deleteFolderRecursive(comicFolder + "/chapters/" + oldResourcePath + "/");
            }

            // Step 5: Update DB
            chapter.setResourcePath(newFinalFolder);
            chapter.setImageCount(slot);
            chapterRepository.save(chapter);
            refreshLastChapterAt(chapter.getComic());

        } catch (Exception e) {
            minioService.deleteFolderRecursive(tempPrefix);
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
}

