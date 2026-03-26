package com.gifo.backend.service.bgm;

import com.gifo.backend.global.exception.storage.StorageException;
import com.gifo.backend.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BgmCleanupEventListener {

    private final StorageService storageService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleBgmCleanup(BgmCleanupEvent event) {
        List<String> uploadedBgmUrls = event.getUploadedBgmUrls();
        String selectedBgmUrl = event.getSelectedBgmUrl();

        if (uploadedBgmUrls == null || uploadedBgmUrls.isEmpty()) return;

        uploadedBgmUrls.stream()
                .filter(url -> !url.equals(selectedBgmUrl))
                .forEach(url -> {
                    try {
                        storageService.deleteBgm(url);
                    } catch (StorageException e) {
                        log.warn("업로드된 BGM 삭제 실패: {}", url, e);
                    }
                });
    }
}
