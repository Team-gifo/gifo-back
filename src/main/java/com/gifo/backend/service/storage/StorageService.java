package com.gifo.backend.service.storage;

import com.gifo.backend.dto.storage.ImageType;
import com.gifo.backend.global.ErrorCode;
import com.gifo.backend.global.exception.storage.StorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
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
        } catch (IOException | S3Exception | SdkClientException e) {
            log.error("[STORAGE] upload failed: {} - {}", e.getClass().getName(), e.getMessage(), e);
            throw new StorageException(ErrorCode.STORAGE_UPLOAD_FAILED);
        }

        return cdnDomain + "/" + objectKey;
    }

    private byte[] compress(MultipartFile file) throws IOException {
        try {
            // webp-imageio가 ServiceLoader로 등록되어 WebP 포함 모든 포맷 지원
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
            if (original == null) {
                throw new IOException("지원하지 않는 이미지 포맷입니다.");
            }

            // JPEG는 알파 채널 미지원 → 흰 배경 RGB로 변환
            BufferedImage rgb = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = rgb.createGraphics();
            try {
                g.drawImage(original, 0, 0, java.awt.Color.WHITE, null);
            } finally {
                g.dispose();
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Thumbnails.of(rgb)
                    .size(1920, 1920)
                    .keepAspectRatio(true)
                    .outputFormat("JPEG")
                    .outputQuality(0.8)
                    .toOutputStream(output);
            return output.toByteArray();
        } catch (Exception e) {
            throw new IOException("이미지 파일을 읽을 수 없습니다: " + e.getMessage(), e);
        }
    }

    public void delete(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith(cdnDomain + "/")) {
            throw new StorageException(ErrorCode.INVALID_IMAGE_URL);
        }
        String objectKey = imageUrl.substring((cdnDomain + "/").length());
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

}
