package com.gifo.backend.dto.event;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 이벤트 조회 응답 DTO (GET /events/{eventUrl})
 * 프론트엔드 ContentData 모델과 1:1 매핑되는 구조
 */
public class EventResponse {

    /**
     * 최상위 응답 - 이벤트 전체 콘텐츠
     * URL 접속 시 갤러리 + 이벤트 타입(가차/퀴즈/언박싱) 한번에 반환
     */
    public record Content(
            String user,            // 생일 주인공 이름
            String subTitle,        // 이벤트 제목
            String bgm,             // BGM URL
            List<GalleryItem> gallery,      // 추억 갤러리 (없으면 빈 리스트)
            ContentWrapper content          // 가차/퀴즈/언박싱 중 1개만 non-null
    ) {}

    /**
     * 추억 갤러리 아이템 (Memory 엔티티 → 응답 매핑)
     */
    public record GalleryItem(
            String title,
            String imageUrl,
            String description
    ) {}

    /**
     * 콘텐츠 타입 래퍼 - 세 타입 중 하나만 non-null
     * 가차(capsuleEvent) / 퀴즈(quizEvent) / 언박싱(directEvent)
     */
    public record ContentWrapper(
            GachaContent gacha,
            QuizContent quiz,
            UnboxingContent unboxing
    ) {}

    // ── 가차 (캡슐 뽑기) ───────────────────────────────

    /**
     * 캡슐 뽑기 콘텐츠
     * playCount: 이벤트 생성 시 설정된 총 뽑기 횟수
     * remainingDrawCount: 현재 남은 뽑기 횟수 (재접속 판단용)
     */
    public record GachaContent(
            int playCount,
            int remainingDrawCount,
            boolean selected,
            List<GachaItem> list,
            List<DrawHistoryItem> drawHistory
    ) {}

    /**
     * 뽑기 이력 아이템 (재접속 시 히스토리 복원용)
     */
    public record DrawHistoryItem(
            Long capsuleId,
            String giftName,
            String giftImageUrl,
            String description,
            boolean selected,
            LocalDateTime drawnAt
    ) {}

    /**
     * 캡슐 아이템 (선물 1개의 확률 정보)
     * percent: 가중치 기반 실제 확률 (0.0~1.0)
     * percentOpen: true면 확률 공개, false면 비공개
     */
    public record GachaItem(
            String itemName,
            String imageUrl,
            double percent,
            boolean percentOpen
    ) {}

    // ── 퀴즈 ─────────────────────────────────────────

    /**
     * 퀴즈 콘텐츠
     * successReward: 정답 수 >= requiredCount 일 때 받는 선물
     * failReward: 그 미만일 때 받는 선물
     */
    public record QuizContent(
            int currentQuizIndex,
            Integer remainingAttempts,
            RewardItem successReward,
            RewardItem failReward,
            List<QuizItem> list,
            List<AnswerHistoryItem> answerHistory
    ) {}

    /**
     * 퀴즈 풀이 이력 아이템 (재접속 시 복원용)
     * quizId: 문제 PK
     * correct: 정답 여부
     */
    public record AnswerHistoryItem(
            Long quizId,
            boolean correct
    ) {}

    /**
     * 보상 선물 정보
     * requiredCount: 성공에 필요한 최소 정답 수 (failReward는 null)
     */
    public record RewardItem(
            Integer requiredCount,
            String itemName,
            String imageUrl
    ) {}

    /**
     * 퀴즈 문항 1개
     * type: "multiple_choice" | "ox" | "text" (프론트 매핑용)
     * options: 선택지 목록 (주관식은 빈 리스트)
     * 정답은 서버에서 채점하므로 클라이언트에 노출하지 않음
     */
    public record QuizItem(
            Long quizId,
            String type,
            String title,
            String imageUrl,
            String description,
            String hint,
            List<String> options,
            int playLimit
    ) {}

    // ── 언박싱 (확정 선물) ──────────────────────────────

    /**
     * 확정 선물 콘텐츠
     * 별도 API 없이 초기 응답에 before/after 모두 포함
     * 프론트에서 UI 애니메이션으로 개봉 처리
     */
    public record UnboxingContent(
            BeforeOpen beforeOpen,
            AfterOpen afterOpen
    ) {}

    /**
     * 개봉 전 화면 정보
     */
    public record BeforeOpen(
            String imageUrl,
            String description
    ) {}

    /**
     * 개봉 후 선물 정보
     */
    public record AfterOpen(
            String itemName,
            String imageUrl
    ) {}
}
