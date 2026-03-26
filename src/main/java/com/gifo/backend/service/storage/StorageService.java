package com.gifo.backend.service.storage;

import com.gifo.backend.global.ErrorCode;
import com.gifo.backend.global.exception.storage.StorageException;
import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StorageService {

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of("image/jpeg", "image/png");
    private static final List<String> ALLOWED_EXTENSIONS = List.of(".jpg", ".jpeg", ".png");

    private final S3Client s3Client;

    @Value("${storage.bucket-name}")
    private String bucketName;

    @Value("${storage.cdn-domain}")
    private String cdnDomain;

    public String upload(MultipartFile file, ImageType type) {
        validate(file);

        String objectKey = type.getPrefix() + UUID.randomUUID() + ".jpg";

        try {
            byte[] compressed = compress(file);

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType("image/jpeg")
                    .contentLength((long) compressed.length)
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(compressed));
        } catch (IOException e) {
            throw new StorageException(ErrorCode.STORAGE_UPLOAD_FAILED);
        }

        return cdnDomain + "/" + objectKey;
    }

    private byte[] compress(MultipartFile file) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Thumbnails.of(file.getInputStream())
                .size(1920, 1920)
                .keepAspectRatio(true)
                .outputFormat("JPEG")
                .outputQuality(0.8)
                .toOutputStream(output);
        return output.toByteArray();
    }

    public void delete(String imageUrl) {
        String objectKey = imageUrl.replace(cdnDomain + "/", "");
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build());
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new StorageException(ErrorCode.EMPTY_FILE);
        }

        String contentType = file.getContentType();
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        String extension = extractExtension(filename);

        if (!ALLOWED_CONTENT_TYPES.contains(contentType) || !ALLOWED_EXTENSIONS.contains(extension)) {
            throw new StorageException(ErrorCode.INVALID_FILE_TYPE);
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new StorageException(ErrorCode.INVALID_FILE_TYPE);
        }
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }

    public enum ImageType {
        MEMORY("memories/"),
        GIFT("gifts/"),
        QUIZ("quizzes/");

        private final String prefix;

        ImageType(String prefix) {
            this.prefix = prefix;
        }

        public String getPrefix() {
            return prefix;
        }
    }
}
