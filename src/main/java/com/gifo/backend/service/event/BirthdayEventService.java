package com.gifo.backend.service.event;

import com.gifo.backend.dto.event.BirthdayEventCreateRequest;
import com.gifo.backend.dto.event.BirthdayEventCreateResponse;
import com.gifo.backend.dto.event.EventResponse;
import com.gifo.backend.entity.capsule.Capsule;
import com.gifo.backend.entity.capsule.CapsuleDraw;
import com.gifo.backend.entity.capsule.CapsuleEvent;
import com.gifo.backend.entity.direct.DirectEvent;
import com.gifo.backend.entity.event.BirthdayEvent;
import com.gifo.backend.entity.event.EventStatus;
import com.gifo.backend.entity.event.Memory;
import com.gifo.backend.entity.gift.Gift;
import com.gifo.backend.entity.quiz.*;
import com.gifo.backend.global.ErrorCode;
import com.gifo.backend.global.exception.event.EventException;
import com.gifo.backend.global.util.EntityFinder;
import com.gifo.backend.service.bgm.BgmCleanupEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import com.gifo.backend.repository.capsule.CapsuleDrawRepository;
import com.gifo.backend.repository.capsule.CapsuleEventRepository;
import com.gifo.backend.repository.capsule.CapsuleRepository;
import com.gifo.backend.repository.direct.DirectEventRepository;
import com.gifo.backend.repository.event.BirthdayEventRepository;
import com.gifo.backend.repository.event.MemoryRepository;
import com.gifo.backend.repository.gift.GiftRepository;
import com.gifo.backend.repository.quiz.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BirthdayEventService {

    private final BirthdayEventRepository birthdayEventRepository;
    private final MemoryRepository memoryRepository;
    private final GiftRepository giftRepository;
    private final CapsuleEventRepository capsuleEventRepository;
    private final CapsuleRepository capsuleRepository;
    private final CapsuleDrawRepository capsuleDrawRepository;
    private final DirectEventRepository directEventRepository;
    private final QuizEventRepository quizEventRepository;
    private final QuizRepository quizRepository;
    private final QuizChoiceRepository quizChoiceRepository;
    private final QuizRewardRuleRepository quizRewardRuleRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final EntityFinder entityFinder;
    private final ApplicationEventPublisher eventPublisher;

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 8;
    private static final int MAX_RETRY = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    public BirthdayEventCreateResponse createEvent(BirthdayEventCreateRequest req) {

        if (req.getUploadedBgmUrls() != null && req.getUploadedBgmUrls().size() > 3) {
            throw new EventException(ErrorCode.BGM_UPLOAD_LIMIT_EXCEEDED);
        }

        BirthdayEvent event = birthdayEventRepository.save(
                BirthdayEvent.builder()
                        .eventUrl(generateUniqueEventUrl())
                        .status(EventStatus.ACTIVE)
                        .receiverName(req.getUser())
                        .senderName(req.getSenderName())
                        .password(req.getPassword())
                        .title(req.getSubTitle())
                        .bgmUrl(req.getBgm())
                        .createdAt(LocalDateTime.now())
                        .expiredAt(req.getExpiredAt())
                        .build()
        );

        saveGallery(event, req.getGallery());

        if (req.getContent() != null) {
            if (req.getContent().getGacha() != null) saveGacha(event, req.getContent().getGacha());
            if (req.getContent().getQuiz() != null) saveQuiz(event, req.getContent().getQuiz());
            if (req.getContent().getUnboxing() != null) saveUnboxing(event, req.getContent().getUnboxing());
        }

        eventPublisher.publishEvent(new BgmCleanupEvent(this, req.getUploadedBgmUrls(), req.getBgm()));

        return BirthdayEventCreateResponse.builder()
                .eventId(event.getEventId())
                .eventUrl(event.getEventUrl())
                .build();
    }

    // ══════════════════════════════════════════════
    // 이벤트 조회 (GET /events/{eventUrl})
    // - URL로 이벤트 조회 + ACTIVE 상태 검증
    // - 추억 갤러리(없으면 빈 리스트) + 콘텐츠(가차/퀴즈/언박싱 중 1개) 반환
    // ══════════════════════════════════════════════

    @Transactional(readOnly = true)
    public EventResponse.Content getEventContent(String eventUrl) {
        BirthdayEvent event = entityFinder.getEventByUrlOrThrow(eventUrl);

        // 추억 갤러리 조회 (없으면 빈 리스트)
        List<EventResponse.GalleryItem> gallery =
                memoryRepository.findByBirthdayEventOrderBySortOrderAsc(event).stream()
                        .map(m -> new EventResponse.GalleryItem(
                                m.getTitle(), m.getImageUrl(), m.getDescription()))
                        .toList();

        // 이벤트 타입 분기 (세 타입 중 세팅된 1개만 non-null)
        EventResponse.ContentWrapper content = null;

        if (event.getCapsuleEvent() != null) {
            content = new EventResponse.ContentWrapper(
                    buildGachaContent(event.getCapsuleEvent()), null, null);

        } else if (event.getQuizEvent() != null) {
            content = new EventResponse.ContentWrapper(
                    null, buildQuizContent(event.getQuizEvent()), null);

        } else if (event.getDirectEvent() != null) {
            content = new EventResponse.ContentWrapper(
                    null, null, buildUnboxingContent(event.getDirectEvent()));
        }

        return new EventResponse.Content(
                event.getReceiverName(),
                event.getTitle(),
                event.getBgmUrl(),
                gallery,
                content
        );
    }

    // ── 가차 콘텐츠 빌드 ─────────────────────────────
    // 이미 뽑힌 캡슐 제외 → 남은 캡슐만 반환 + 남은 뽑기 횟수 계산 + 뽑기 히스토리
    private EventResponse.GachaContent buildGachaContent(CapsuleEvent capsuleEvent) {
        List<Capsule> allCapsules = capsuleRepository.findByCapsuleEvent(capsuleEvent);
        // 이미 뽑힌 캡슐 ID Set으로 필터링 (O(1) lookup)
        Set<Long> drawnIds = capsuleDrawRepository.findDrawnCapsuleIdsByCapsuleEvent(capsuleEvent);
        List<Capsule> remainingCapsules = allCapsules.stream()
                .filter(c -> !drawnIds.contains(c.getCapsuleId()))
                .toList();

        int totalWeight = remainingCapsules.stream().mapToInt(Capsule::getWeight).sum();
        long drawCount = capsuleDrawRepository.countByCapsuleEvent(capsuleEvent);
        int remainingDraws = Math.min(
                capsuleEvent.getMaxDrawCount() - (int) drawCount,
                remainingCapsules.size()
        );

        List<EventResponse.GachaItem> items = remainingCapsules.stream()
                .map(c -> new EventResponse.GachaItem(
                        c.getGift().getGiftName(),
                        c.getGift().getGiftImageUrl(),
                        totalWeight > 0 ? (double) c.getWeight() / totalWeight : 0.0,
                        c.getGift().getIsProbabilityPublic()))
                .toList();

        // 뽑기 히스토리 (재접속 시 복원용)
        List<CapsuleDraw> draws = capsuleDrawRepository.findByCapsuleEventWithGift(capsuleEvent);
        boolean hasSelected = draws.stream().anyMatch(CapsuleDraw::getSelected);

        List<EventResponse.DrawHistoryItem> drawHistory = draws.stream()
                .map(d -> new EventResponse.DrawHistoryItem(
                        d.getCapsule().getCapsuleId(),
                        d.getCapsule().getGift().getGiftName(),
                        d.getCapsule().getGift().getGiftImageUrl(),
                        d.getCapsule().getGift().getDescription(),
                        d.getSelected(),
                        d.getCreatedAt()))
                .toList();

        return new EventResponse.GachaContent(
                capsuleEvent.getMaxDrawCount(), remainingDraws, hasSelected, items, drawHistory);
    }

    // ── 퀴즈 콘텐츠 빌드 ─────────────────────────────
    // 성공/실패 보상 + 문항별 선택지·정답 구성
    private EventResponse.QuizContent buildQuizContent(QuizEvent quizEvent) {
        List<QuizRewardRule> rules =
                quizRewardRuleRepository.findByQuizEventOrderByMinCorrectDesc(quizEvent);

        // 성공 보상: minCorrect > 0 인 규칙
        QuizRewardRule successRule = rules.stream()
                .filter(r -> r.getMinCorrect() != null && r.getMinCorrect() > 0)
                .findFirst().orElse(null);
        // 실패 보상: minCorrect = 0 인 규칙
        QuizRewardRule failRule = rules.stream()
                .filter(r -> r.getMinCorrect() == null || r.getMinCorrect() == 0)
                .findFirst().orElse(null);

        EventResponse.RewardItem successReward = successRule == null ? null :
                new EventResponse.RewardItem(
                        successRule.getMinCorrect(),
                        successRule.getGift().getGiftName(),
                        successRule.getGift().getGiftImageUrl());

        EventResponse.RewardItem failReward = failRule == null ? null :
                new EventResponse.RewardItem(
                        null,
                        failRule.getGift().getGiftName(),
                        failRule.getGift().getGiftImageUrl());

        List<EventResponse.QuizItem> quizItems =
                quizRepository.findByQuizEventOrderBySortOrderAsc(quizEvent).stream()
                        .map(this::buildQuizItem)
                        .toList();

        // 퀴즈 풀이 이력 (재접속 시 복원용)
        List<QuizAnswer> answers = quizAnswerRepository.findByQuizEventOrderByQuizAnswerIdAsc(quizEvent);
        int currentQuizIndex = answers.size();
        Integer remainingAttempts = quizEvent.getCurrentQuizRemainingAttempts();

        List<EventResponse.AnswerHistoryItem> answerHistory = answers.stream()
                .map(a -> new EventResponse.AnswerHistoryItem(
                        a.getQuiz().getQuizId(),
                        a.getCorrect()))
                .toList();

        return new EventResponse.QuizContent(
                currentQuizIndex, remainingAttempts,
                successReward, failReward, quizItems, answerHistory);
    }

    // 퀴즈 문항 빌드: 타입별 선택지 구성 (정답은 서버 채점이므로 미노출)
    private EventResponse.QuizItem buildQuizItem(Quiz quiz) {
        List<QuizChoice> choices = quiz.getQuizChoices();

        List<String> options = switch (quiz.getQuizType()) {
            case OBJECTIVE -> choices.stream().map(QuizChoice::getChoiceText).toList();
            case OX -> List.of("O", "X");
            case SUBJECTIVE -> List.of();
        };

        return new EventResponse.QuizItem(
                quiz.getQuizId(),
                reverseMapQuizType(quiz.getQuizType()),
                quiz.getQuestion(),
                quiz.getImageUrl(),
                quiz.getDescription(),
                quiz.getHint(),
                options,
                quiz.getPlayLimit()
        );
    }

    // DB 타입 → 프론트 타입 역매핑
    private String reverseMapQuizType(QuizType quizType) {
        return switch (quizType) {
            case OBJECTIVE -> "multiple_choice";
            case OX -> "ox";
            case SUBJECTIVE -> "text";
        };
    }

    // ── 언박싱 콘텐츠 빌드 ───────────────────────────
    // 별도 API 없이 before(개봉 전 화면) + after(선물) 함께 반환
    private EventResponse.UnboxingContent buildUnboxingContent(DirectEvent directEvent) {
        Gift gift = directEvent.getGift();
        return new EventResponse.UnboxingContent(
                new EventResponse.BeforeOpen(
                        directEvent.getBeforeImageUrl(),
                        directEvent.getBeforeDescription()),
                new EventResponse.AfterOpen(
                        gift.getGiftName(),
                        gift.getGiftImageUrl())
        );
    }

    // ══════════════════════════════════════════════
    // 진행 데이터 초기화 (DELETE /events/{eventUrl}/progress)
    // - 재접속 시 "다시 시작하겠습니까?" → 기존 뽑기/퀴즈 이력 삭제
    // - 캡슐: capsule_draw 전체 삭제
    // - 퀴즈: quiz_answer 전체 삭제 + totalAttempt/remainingAttempts 초기화
    // - 언박싱: 서버 측 초기화 데이터 없음
    // ══════════════════════════════════════════════

    public void resetProgress(String eventUrl) {
        BirthdayEvent event = entityFinder.getEventByUrlForUpdateOrThrow(eventUrl);

        if (event.getCapsuleEvent() != null) {
            capsuleDrawRepository.deleteByCapsuleEvent(event.getCapsuleEvent());
        } else if (event.getQuizEvent() != null) {
            QuizEvent quizEvent = event.getQuizEvent();
            quizAnswerRepository.deleteByQuizEvent(quizEvent);
            quizEvent.setTotalAttempt(0);
            quizEvent.setLastCorrectCount(null);
            quizEvent.setLastSuccess(null);
            quizEvent.setCurrentQuizRemainingAttempts(null);
        } else if (event.getDirectEvent() != null) {
        }
    }

    // ──────────────────────────────────────────────
    // Gallery → Memory
    // ──────────────────────────────────────────────

    private void saveGallery(BirthdayEvent event, List<BirthdayEventCreateRequest.GalleryItemDto> gallery) {
        if (gallery == null) return;
        for (int i = 0; i < gallery.size(); i++) {
            BirthdayEventCreateRequest.GalleryItemDto item = gallery.get(i);
            memoryRepository.save(Memory.builder()
                    .birthdayEvent(event)
                    .title(item.getTitle())
                    .imageUrl(item.getImageUrl())
                    .description(item.getDescription())
                    .sortOrder(i + 1)
                    .createdAt(LocalDateTime.now())
                    .build());
        }
    }

    // ──────────────────────────────────────────────
    // Gacha → CapsuleEvent + Capsule + Gift
    // ──────────────────────────────────────────────

    private void saveGacha(BirthdayEvent event, BirthdayEventCreateRequest.GachaDto gacha) {
        CapsuleEvent capsuleEvent = capsuleEventRepository.save(
                CapsuleEvent.builder()
                        .birthdayEvent(event)
                        .maxDrawCount(gacha.getPlayCount())
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        if (gacha.getList() == null) return;
        for (BirthdayEventCreateRequest.GachaItemDto item : gacha.getList()) {
            Gift gift = giftRepository.save(Gift.builder()
                    .giftName(item.getItemName())
                    .giftImageUrl(item.getImageUrl())
                    .isHidden(false)
                    .isProbabilityPublic(item.isPercentOpen())
                    .createdAt(LocalDateTime.now())
                    .build());

            capsuleRepository.save(Capsule.builder()
                    .capsuleEvent(capsuleEvent)
                    .gift(gift)
                    .weight((int) (item.getPercent() * 10000)) // 0.001 → 10, 1.0 → 10000
                    .createdAt(LocalDateTime.now())
                    .build());
        }
    }

    // ──────────────────────────────────────────────
    // Quiz → QuizEvent + Quiz + QuizChoice + QuizRewardRule + Gift
    // ──────────────────────────────────────────────

    private void saveQuiz(BirthdayEvent event, BirthdayEventCreateRequest.QuizDto quizDto) {
        QuizEvent quizEvent = quizEventRepository.save(
                QuizEvent.builder()
                        .birthdayEvent(event)
                        .totalAttempt(0)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        // 성공 보상
        if (quizDto.getSuccessReward() != null) {
            Gift successGift = saveGift(quizDto.getSuccessReward());
            quizRewardRuleRepository.save(QuizRewardRule.builder()
                    .quizEvent(quizEvent)
                    .minCorrect(quizDto.getSuccessReward().getRequiredCount())
                    .gift(successGift)
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        // 실패 보상 (minCorrect = 0)
        if (quizDto.getFailReward() != null) {
            Gift failGift = saveGift(quizDto.getFailReward());
            quizRewardRuleRepository.save(QuizRewardRule.builder()
                    .quizEvent(quizEvent)
                    .minCorrect(0)
                    .gift(failGift)
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        // 퀴즈 문항
        if (quizDto.getList() == null) return;
        for (BirthdayEventCreateRequest.QuizItemDto item : quizDto.getList()) {
            Quiz quiz = quizRepository.save(Quiz.builder()
                    .quizEvent(quizEvent)
                    .question(item.getTitle())
                    .quizType(mapQuizType(item.getType()))
                    .hint(item.getHint())
                    .imageUrl(item.getImageUrl())
                    .description(item.getDescription())
                    .playLimit(item.getPlayLimit())
                    .sortOrder(item.getQuizId())
                    .createdAt(LocalDateTime.now())
                    .build());

            saveQuizChoices(quiz, item);
        }
    }

    private Gift saveGift(BirthdayEventCreateRequest.RewardDto reward) {
        return giftRepository.save(Gift.builder()
                .giftName(reward.getItemName())
                .giftImageUrl(reward.getImageUrl())
                .isHidden(false)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private void saveQuizChoices(Quiz quiz, BirthdayEventCreateRequest.QuizItemDto item) {
        switch (quiz.getQuizType()) {
            case OBJECTIVE -> {
                // 선택지 전체 저장, 정답 여부 표시
                for (String option : item.getOptions()) {
                    quizChoiceRepository.save(QuizChoice.builder()
                            .quiz(quiz)
                            .choiceText(option)
                            .isCorrect(item.getAnswer().contains(option))
                            .build());
                }
            }
            case OX -> {
                // O / X 두 선택지 저장
                for (String option : List.of("O", "X")) {
                    quizChoiceRepository.save(QuizChoice.builder()
                            .quiz(quiz)
                            .choiceText(option)
                            .isCorrect(item.getAnswer().contains(option))
                            .build());
                }
            }
            case SUBJECTIVE -> {
                // 정답 텍스트 목록을 모두 is_correct=true 로 저장 (다양한 표기 허용)
                for (String answer : item.getAnswer()) {
                    quizChoiceRepository.save(QuizChoice.builder()
                            .quiz(quiz)
                            .choiceText(answer)
                            .isCorrect(true)
                            .build());
                }
            }
        }
    }

    private QuizType mapQuizType(String type) {
        return switch (type) {
            case "multiple_choice" -> QuizType.OBJECTIVE;
            case "ox" -> QuizType.OX;
            default -> QuizType.SUBJECTIVE;
        };
    }

    // ──────────────────────────────────────────────
    // Unboxing → DirectEvent + Gift
    // ──────────────────────────────────────────────

    private void saveUnboxing(BirthdayEvent event, BirthdayEventCreateRequest.UnboxingDto unboxing) {
        Gift gift = giftRepository.save(Gift.builder()
                .giftName(unboxing.getAfterOpen().getItemName())
                .giftImageUrl(unboxing.getAfterOpen().getImageUrl())
                .isHidden(true) // 개봉 전까지 숨김
                .createdAt(LocalDateTime.now())
                .build());

        directEventRepository.save(DirectEvent.builder()
                .birthdayEvent(event)
                .gift(gift)
                .beforeImageUrl(unboxing.getBeforeOpen().getImageUrl())
                .beforeDescription(unboxing.getBeforeOpen().getDescription())
                .createdAt(LocalDateTime.now())
                .build());
    }

    // ──────────────────────────────────────────────
    // 고유 eventUrl 생성 (8자리 영문 대문자 + 숫자)
    // ──────────────────────────────────────────────

    private String generateUniqueEventUrl() {
        for (int attempt = 0; attempt < MAX_RETRY; attempt++) {
            String code = generateRandomCode();
            if (!birthdayEventRepository.existsByEventUrl(code)) {
                return code;
            }
        }
        throw new EventException(ErrorCode.EVENT_URL_GENERATION_FAILED);
    }

    private String generateRandomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
