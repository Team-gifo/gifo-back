package com.gifo.backend.controller.bgm;

import com.gifo.backend.dto.bgm.BgmPresetResponse;
import com.gifo.backend.dto.bgm.BgmUploadResponse;
import com.gifo.backend.global.ApiResponse;
import com.gifo.backend.service.bgm.BgmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/bgm")
@Tag(name = "BGM API", description = "배경 음악 업로드 및 프리셋 조회 API")
@RequiredArgsConstructor
public class BgmController {

    private final BgmService bgmService;

    @GetMapping("/presets")
    @Operation(summary = "프리셋 BGM 목록 조회", description = "앱 제공 프리셋 BGM 3개의 ID, 이름, URL을 반환합니다.")
    public ResponseEntity<ApiResponse<List<BgmPresetResponse>>> getPresets() {
        return ResponseEntity.ok(ApiResponse.success("프리셋 BGM 조회 성공", bgmService.getPresets()));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "BGM 업로드", description = "오디오 파일(MP3, WAV, OGG, AAC)을 업로드하고 CDN URL을 반환합니다. 최대 20MB.")
    public ResponseEntity<ApiResponse<BgmUploadResponse>> upload(
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(ApiResponse.success("BGM 업로드 성공", bgmService.upload(file)));
    }
}
