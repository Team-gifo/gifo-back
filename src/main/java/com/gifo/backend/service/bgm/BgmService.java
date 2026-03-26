package com.gifo.backend.service.bgm;

import com.gifo.backend.dto.bgm.BgmPreset;
import com.gifo.backend.dto.bgm.BgmPresetResponse;
import com.gifo.backend.dto.bgm.BgmUploadResponse;
import com.gifo.backend.dto.storage.AudioType;
import com.gifo.backend.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BgmService {

    private final StorageService storageService;

    @Value("${storage.cdn-domain}")
    private String cdnDomain;

    public List<BgmPresetResponse> getPresets() {
        return Arrays.stream(BgmPreset.values())
                .map(p -> new BgmPresetResponse(p.getId(), p.getName(), cdnDomain + "/" + p.getPath()))
                .toList();
    }

    public BgmUploadResponse upload(MultipartFile file) {
        String bgmUrl = storageService.uploadAudio(file, AudioType.BGM);
        return new BgmUploadResponse(bgmUrl);
    }
}
