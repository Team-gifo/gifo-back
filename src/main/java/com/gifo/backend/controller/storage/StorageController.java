package com.gifo.backend.controller.storage;

import com.gifo.backend.dto.storage.ImageUploadResponse;
import com.gifo.backend.global.ApiResponse;
import com.gifo.backend.service.storage.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/images")
@Tag(name = "Image API", description = "이미지 업로드 API")
@RequiredArgsConstructor
public class StorageController {

    private final StorageService storageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "이미지 업로드", description = "이미지를 업로드하고 CDN URL을 반환합니다. type: MEMORY, GIFT, QUIZ")
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") StorageService.ImageType type
    ) {
        String imageUrl = storageService.upload(file, type);
        return ResponseEntity.ok(ApiResponse.success("이미지 업로드 성공", new ImageUploadResponse(imageUrl)));
    }
}
