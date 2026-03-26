package com.gifo.backend.service.bgm;

import org.springframework.context.ApplicationEvent;

import java.util.List;

public class BgmCleanupEvent extends ApplicationEvent {

    private final List<String> uploadedBgmUrls;
    private final String selectedBgmUrl;

    public BgmCleanupEvent(Object source, List<String> uploadedBgmUrls, String selectedBgmUrl) {
        super(source);
        this.uploadedBgmUrls = uploadedBgmUrls;
        this.selectedBgmUrl = selectedBgmUrl;
    }

    public List<String> getUploadedBgmUrls() {
        return uploadedBgmUrls;
    }

    public String getSelectedBgmUrl() {
        return selectedBgmUrl;
    }
}
