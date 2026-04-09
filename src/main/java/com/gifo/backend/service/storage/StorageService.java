package com.gifo.backend.service.storage;

import com.gifo.backend.dto.storage.AudioType;
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
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of("image/jpeg", "image/png");
    private static final List<String> ALLOWED_EXTENSIONS = List.of(".jpg", ".jpeg", ".png");

    private static final Map<String, List<String>> AUDIO_EXTENSION_TO_CONTENT_TYPES = Map.ofEntries(
            Map.entry(".mp3", List.of("audio/mpeg")),
            Map.entry(".wav", List.of("audio/wav")),
            Map.entry(".ogg", List.of("audio/ogg")),
            Map.entry(".aac", List.of("audio/aac")),
            Map.entry(".m4a", List.of("audio/mp4", "audio/x-m4a"))
    );
    private static final long MAX_AUDIO_SIZE_BYTES = 20 * 1024 * 1024L; // 20MB

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

    public String uploadAudio(MultipartFile file, AudioType type) {
        validateAudio(file);

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        String extension = extractExtension(originalFilename);
        String objectKey = type.getPrefix() + UUID.randomUUID() + extension;

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException | S3Exception | SdkClientException e) {
            throw new StorageException(ErrorCode.STORAGE_UPLOAD_FAILED);
        }

        return cdnDomain + "/" + objectKey;
    }

    public void deleteBgm(String bgmUrl) {
        String normalizedDomain = cdnDomain.endsWith("/") ? cdnDomain.substring(0, cdnDomain.length() - 1) : cdnDomain;
        if (bgmUrl == null || !bgmUrl.startsWith(normalizedDomain + "/")) {
            throw new StorageException(ErrorCode.INVALID_BGM_URL);
        }
        String objectKey = bgmUrl.substring((normalizedDomain + "/").length());
        if (!objectKey.startsWith("bgm/")) {
            throw new StorageException(ErrorCode.INVALID_BGM_URL);
        }
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build());
        } catch (S3Exception | SdkClientException e) {
            throw new StorageException(ErrorCode.STORAGE_DELETE_FAILED);
        }
    }

    private byte[] compress(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        // JPEG/PNG 지원 (WebP는 validate()에서 거부됨)
        BufferedImage original = ImageIO.read(new ByteArrayInputStream(bytes));
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

        // 확장자와 무관하게 실제 WebP 파일 거부 (RIFF....WEBP 시그니처) — 12바이트만 읽어 메모리 절약
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[12];
            int read = is.read(header);
            if (read == 12
                    && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                    && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
                throw new StorageException(ErrorCode.INVALID_FILE_TYPE);
            }
        } catch (StorageException e) {
            throw e;
        } catch (IOException e) {
            throw new StorageException(ErrorCode.EMPTY_FILE);
        }
    }

    private void validateAudio(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new StorageException(ErrorCode.EMPTY_FILE);
        }

        if (file.getSize() > MAX_AUDIO_SIZE_BYTES) {
            throw new StorageException(ErrorCode.FILE_SIZE_EXCEEDED);
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            throw new StorageException(ErrorCode.INVALID_AUDIO_FILE_TYPE);
        }
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        String extension = extractExtension(filename);

        List<String> allowedContentTypes = AUDIO_EXTENSION_TO_CONTENT_TYPES.getOrDefault(extension, List.of());
        if (allowedContentTypes.isEmpty() || !allowedContentTypes.contains(contentType)) {
            throw new StorageException(ErrorCode.INVALID_AUDIO_FILE_TYPE);
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new StorageException(ErrorCode.INVALID_FILE_TYPE);
        }
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }

}
