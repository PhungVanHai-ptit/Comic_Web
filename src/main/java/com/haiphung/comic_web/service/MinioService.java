package com.haiphung.comic_web.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MinioService {

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.url}")
    private String minioUrl;

    private final String AVATAR_BUCKET = "avatars";
    private final String COMIC_BUCKET = "comics";

    // upload avatar
    public String uploadAvatar(Integer userId, MultipartFile file) throws Exception {

        String objectName = "user-" + userId +"-"+ System.currentTimeMillis() + ".jpg";

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(AVATAR_BUCKET)
                        .object(objectName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );

        return minioUrl + "/" + AVATAR_BUCKET + "/" + objectName;
    }

    // delete avatar from minio
    public void deleteAvatar(String avatarUrl) throws Exception {
        if (avatarUrl == null || avatarUrl.isEmpty()) {
            return;
        }

        try {
            // Extract object name from URL
            // URL format: http://localhost:9000/avatars/user-{id}{timestamp}.jpg
            String objectName = avatarUrl.replace(minioUrl + "/" + AVATAR_BUCKET + "/", "");
            
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(AVATAR_BUCKET)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            // Log but don't throw - deletion failure shouldn't prevent new upload
            System.err.println("Error deleting old avatar: " + e.getMessage());
        }
    }

    // upload ảnh chapter
    public void uploadChapterImages(String comic, String chapter, MultipartFile file, int index) throws Exception {

        String objectName = comic + "/" + chapter + "/" + index + ".jpg";

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(COMIC_BUCKET)
                        .object(objectName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );
    }

    // upload ảnh bìa truyện
    public String uploadComicCover(Integer comicId, MultipartFile file) throws Exception {
        String objectName = "comic-" + comicId + "/cover-" + System.currentTimeMillis() + ".jpg";

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(COMIC_BUCKET)
                        .object(objectName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );
        return objectName;
    }

    // xóa ảnh bìa truyện
    public void deleteComicCover(String objectName) throws Exception {
        if (objectName == null || objectName.isEmpty() || objectName.startsWith("http")) {
            return;
        }

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(COMIC_BUCKET)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            System.err.println("Error deleting old cover: " + e.getMessage());
        }
    }

    // BƯỚC 1: Upload ảnh chapter vào thư mục TEMP
    public void uploadTempChapterImage(String comicFolder, String tempFolder, MultipartFile file, int index) throws Exception {
        String objectName = comicFolder + "/chapters/temp/" + tempFolder + "/" + index + ".jpg";
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(COMIC_BUCKET)
                        .object(objectName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );
    }

    // BƯỚC 2: Move toàn bộ ảnh từ TEMP sang THƯ MỤC CHÍNH (Atomic Commit)
    public void commitTempChapter(String comicFolder, String tempFolder, String finalFolder, int imageCount) throws Exception {
        String tempPrefix = comicFolder + "/chapters/temp/" + tempFolder + "/";
        String finalPrefix = comicFolder + "/chapters/" + finalFolder + "/";

        for (int i = 1; i <= imageCount; i++) {
            String sourceObj = tempPrefix + i + ".jpg";
            String destObj = finalPrefix + i + ".jpg";

            // Copy file form temp to final
            minioClient.copyObject(
                    io.minio.CopyObjectArgs.builder()
                            .bucket(COMIC_BUCKET)
                            .object(destObj)
                            .source(io.minio.CopySource.builder().bucket(COMIC_BUCKET).object(sourceObj).build())
                            .build()
            );
        }
        
        // After successful copy, safely delete temp folder
        deleteFolderRecursive(tempPrefix);
    }

    // Xóa một folder nguyên dạng (dùng để rollback nếu step 1 lỗi, hoặc xóa trọn chapter nếu user yêu cầu Delete)
    public void deleteFolderRecursive(String prefixPath) {
        try {
            Iterable<io.minio.Result<io.minio.messages.Item>> results = minioClient.listObjects(
                    io.minio.ListObjectsArgs.builder().bucket(COMIC_BUCKET).prefix(prefixPath).recursive(true).build()
            );
            for (io.minio.Result<io.minio.messages.Item> result : results) {
                minioClient.removeObject(
                        RemoveObjectArgs.builder().bucket(COMIC_BUCKET).object(result.get().objectName()).build()
                );
            }
        } catch (Exception e) {
            System.err.println("Error deleting folder recursively in MinIO: " + e.getMessage());
        }
    }

    // Copy object within the same bucket (used for non-destructive image reorder)
    public void copyObject(String srcObject, String destObject) throws Exception {
        minioClient.copyObject(
                io.minio.CopyObjectArgs.builder()
                        .bucket(COMIC_BUCKET)
                        .object(destObject)
                        .source(io.minio.CopySource.builder().bucket(COMIC_BUCKET).object(srcObject).build())
                        .build()
        );
    }

    // Lấy đuôi file
    public String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg"; // Default
        }
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }

    // Upload file với tên tùy chọn và trả về objectName chuẩn
    public String uploadChapterImageWithCustomName(String comicFolder, String chapterFolder, String customName, MultipartFile file) throws Exception {
        String objectName = comicFolder + "/chapters/" + chapterFolder + "/" + customName;
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(COMIC_BUCKET)
                        .object(objectName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );
        return objectName;
    }

    // Xóa một file cụ thể
    public void deleteFileByPath(String path) throws Exception {
         if (path == null || path.isEmpty()) return;
         minioClient.removeObject(
                 RemoveObjectArgs.builder()
                         .bucket(COMIC_BUCKET)
                         .object(path)
                         .build()
         );
    }
}