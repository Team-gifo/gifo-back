package com.gifo.backend.service.storage;

import com.gifo.backend.dto.storage.AudioType;
import com.gifo.backend.dto.storage.ImageType;
import com.gifo.backend.global.ErrorCode;
import com.gifo.backend.global.exception.storage.StorageException;
import lombok.RequiredArgsConstructor;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StorageService {

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of("image/jpeg", "image/png");
    private static final List<String> ALLOWED_EXTENSIONS = List.of(".jpg", ".jpeg", ".png");

    private static final List<String> ALLOWED_AUDIO_CONTENT_TYPES =
            List.of("audio/mpeg", "audio/wav", "audio/ogg", "audio/aac", "audio/mp4");
    private static final List<String> ALLOWED_AUDIO_EXTENSIONS =
            List.of(".mp3", ".wav", ".ogg", ".aac", ".m4a");
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
        if (bgmUrl == null || !bgmUrl.startsWith(cdnDomain + "/")) {
            throw new StorageException(ErrorCode.INVALID_BGM_URL);
        }
        String objectKey = bgmUrl.substring((cdnDomain + "/").length());
        if (!objectKey.startsWith("bgm/")) {
            throw new StorageException(ErrorCode.INVALID_BGM_URL);
        }
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build());
    }

    private byte[] compress(MultipartFile file) throws IOException {
        try (InputStream in = file.getInputStream();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Thumbnails.of(in)
                    .size(1920, 1920)
                    .keepAspectRatio(true)
                    .outputFormat("JPEG")
                    .outputQuality(0.8)
                    .toOutputStream(output);
            return output.toByteArray();
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

    private void validateAudio(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new StorageException(ErrorCode.EMPTY_FILE);
        }

        if (file.getSize() > MAX_AUDIO_SIZE_BYTES) {
            throw new StorageException(ErrorCode.FILE_SIZE_EXCEEDED);
        }

        String contentType = file.getContentType();
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        String extension = extractExtension(filename);

        if (!ALLOWED_AUDIO_CONTENT_TYPES.contains(contentType) || !ALLOWED_AUDIO_EXTENSIONS.contains(extension)) {
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
