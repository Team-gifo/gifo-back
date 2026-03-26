package com.gifo.backend.dto.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class BirthdayEventCreateRequest {

    /** 생일 주인공 이름 */
    private String user;

    /** 이벤트 생성자 이름 */
    @JsonProperty("sender_name")
    private String senderName;

    private String password;

    /** 페이지 상단 메인 테마 문구 */
    @JsonProperty("sub_title")
    private String subTitle;

    /** 이벤트에 적용할 BGM CDN URL (프리셋 URL 또는 업로드된 URL) */
    private String bgm;

    /**
     * 이벤트 생성 전 업로드한 BGM URL 목록 (최대 3개).
     * 이벤트 생성 완료 후 bgm으로 선택되지 않은 URL은 스토리지에서 삭제를 시도합니다(실패 시 무시될 수 있음).
     */
    @JsonProperty("uploaded_bgm_urls")
    private List<String> uploadedBgmUrls;

    @JsonProperty("expired_at")
    private LocalDateTime expiredAt;

    /** 추억 갤러리 목록 */
    private List<GalleryItemDto> gallery;

    private ContentDto content;

    // ──────────────────────────────────────────────
    // Gallery
    // ──────────────────────────────────────────────

    @Getter
    public static class GalleryItemDto {
        private String title;

        @JsonProperty("image_url")
        private String imageUrl;

        private String description;
    }

    // ──────────────────────────────────────────────
    // Content (gacha / quiz / unboxing)
    // ──────────────────────────────────────────────

    @Getter
    public static class ContentDto {
        private GachaDto gacha;
        private QuizDto quiz;
        private UnboxingDto unboxing;
    }

    // ── Gacha ──

    @Getter
    public static class GachaDto {
        @JsonProperty("play_count")
        private int playCount;

        private List<GachaItemDto> list;
    }

    @Getter
    public static class GachaItemDto {
        @JsonProperty("item_name")
        private String itemName;

        @JsonProperty("image_url")
        private String imageUrl;

        private double percent;

        @JsonProperty("percent_open")
        private boolean percentOpen;
    }

    // ── Quiz ──

    @Getter
    public static class QuizDto {
        @JsonProperty("success_reward")
        private RewardDto successReward;

        @JsonProperty("fail_reward")
        private RewardDto failReward;

        private List<QuizItemDto> list;
    }

    @Getter
    public static class RewardDto {
        /** 성공 기준 정답 수 (fail reward 에서는 사용되지 않음) */
        @JsonProperty("required_count")
        private Integer requiredCount;

        @JsonProperty("item_name")
        private String itemName;

        @JsonProperty("image_url")
        private String imageUrl;
    }

    @Getter
    public static class QuizItemDto {
        @JsonProperty("quiz_id")
        private int quizId;

        /** multiple_choice / subjective / ox */
        private String type;

        private String title;

        @JsonProperty("image_url")
        private String imageUrl;

        private String description;
        private String hint;

        private List<String> options;

        /** 정답 텍스트 목록 (주관식은 여러 형태 허용) */
        private List<String> answer;

        @JsonProperty("play_limit")
        private int playLimit;
    }

    // ── Unboxing ──

    @Getter
    public static class UnboxingDto {
        @JsonProperty("before_open")
        private BeforeOpenDto beforeOpen;

        @JsonProperty("after_open")
        private AfterOpenDto afterOpen;
    }

    @Getter
    public static class BeforeOpenDto {
        @JsonProperty("image_url")
        private String imageUrl;

        private String description;
    }

    @Getter
    public static class AfterOpenDto {
        @JsonProperty("item_name")
        private String itemName;

        @JsonProperty("image_url")
        private String imageUrl;
    }
}
