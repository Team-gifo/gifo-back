package com.gifo.backend.curate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gifo.backend.curate.dto.CurateResponseDto;
import com.gifo.backend.curate.dto.GalleryItemDto;
import com.gifo.backend.curate.dto.SurveyRequestDto;
import com.gifo.backend.curate.dto.content.*;
import com.gifo.backend.curate.prompt.CuratePromptLoader;
import com.gifo.backend.global.exception.CustomException;
import com.gifo.backend.global.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurateService {

    private static final String DEFAULT_BGM = "track_sweet_01";

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final CuratePromptLoader promptLoader;
    private final AzureImageGenerationService imageGenerationService;

    public CurateResponseDto curate(SurveyRequestDto survey) {
        boolean withImages = Boolean.TRUE.equals(survey.getGenerateGalleryImages());
        return curateInternal(survey, withImages);
    }

    public CurateResponseDto curateWithoutImages(SurveyRequestDto survey) {
        return curateInternal(survey, false);
    }

    public CurateResponseDto enrichImages(SurveyRequestDto survey, CurateResponseDto curate) {
        if (curate == null) {
            throw new CustomException(ErrorCode.AI_CURATE_ERROR, "이미지 생성 대상 큐레이션 데이터가 비어 있습니다.");
        }
        enrichGalleryImages(curate, survey);
        return curate;
    }

    private CurateResponseDto curateInternal(SurveyRequestDto survey, boolean withImages) {
        log.info("[Curate] 요청 수신 - relationship={}, situation={}, tone={}, targetAge={}, targetName={}",
                survey.getRelationship(), survey.getSituation(), survey.getTone(),
                survey.getTargetAge(), survey.getTargetName());

        String userMessage = promptLoader.loadPrompt(survey);
        log.debug("[Curate] 생성된 프롬프트 (length={}): {}", userMessage.length(), userMessage);

        try {
            String raw = chatClient.prompt()
                    .user(userMessage)
                    .call()
                    .content();

            if (raw == null || raw.isBlank()) {
                throw new CustomException(ErrorCode.AI_CURATE_ERROR, "AI 응답이 비어 있습니다.");
            }

            String rawPreview = raw.length() > 1000 ? raw.substring(0, 1000) + "..." : raw;
            log.debug("[Curate] Azure OpenAI 원본 응답 (preview): {}", rawPreview);

            String json = stripMarkdownJson(raw);
            log.debug("[Curate] JSON 추출 결과 (length={}): {}", json.length(), json);

            CurateResponseDto result = objectMapper.readValue(json, CurateResponseDto.class);

            if (result == null) {
                throw new CustomException(ErrorCode.AI_CURATE_ERROR, "AI 응답 파싱 결과가 비어 있습니다.");
            }

            if (result.getBgm() == null || result.getBgm().isBlank()) {
                result.setBgm(DEFAULT_BGM);
            }
            if (result.getContent() == null || !hasAnyGame(result.getContent())) {
                result.setContent(buildExampleContent());
            }
            // 갤러리는 1장만 사용하도록 서버에서 최종 강제
            limitGalleryToOne(result);

            if (withImages) {
                enrichGalleryImages(result, survey);
            }

            log.info("[Curate] 큐레이션 생성 완료 - user={}, bgm={}, hasGallery={}, hasGacha={}, hasQuiz={}, hasUnboxing={}",
                    result.getUser(),
                    result.getBgm(),
                    result.getGallery() != null && !result.getGallery().isEmpty(),
                    result.getContent() != null && result.getContent().getGacha() != null,
                    result.getContent() != null && result.getContent().getQuiz() != null,
                    result.getContent() != null && result.getContent().getUnboxing() != null
            );
            return result;
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.warn("AI 큐레이션 생성 실패", e);
            throw new CustomException(ErrorCode.AI_CURATE_ERROR,
                    e.getMessage() != null ? e.getMessage() : ErrorCode.AI_CURATE_ERROR.getDefaultMessage());
        }
    }

    /**
     * LLM이 ```json ... ``` 형태로 감싸서 반환한 경우 껍질을 제거.
     */
    private String stripMarkdownJson(String raw) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf("\n");
            if (start != -1) {
                int end = trimmed.lastIndexOf("```");
                if (end > start) {
                    trimmed = trimmed.substring(start + 1, end).trim();
                } else {
                    trimmed = trimmed.substring(start + 1).trim();
                }
            } else {
                trimmed = trimmed.replaceFirst("^```\\w*", "").replaceFirst("```\\s*$", "").trim();
            }
        }
        return trimmed;
    }

    private void enrichGalleryImages(CurateResponseDto result, SurveyRequestDto survey) {
        if (result.getGallery() == null || result.getGallery().isEmpty()) {
            log.info("[Curate-Image] gallery 항목이 없어 이미지 생성을 건너뜁니다.");
            return;
        }

        log.info("[Curate-Image] gallery 이미지 생성 시작 - itemCount={}", result.getGallery().size());

        for (int i = 0; i < result.getGallery().size(); i++) {
            GalleryItemDto galleryItem = result.getGallery().get(i);
            try {
                String prompt = buildImagePrompt(survey, galleryItem);
                log.info("[Curate-Image] item 이미지 생성 요청 - index={}, title={}, descLength={}, promptLength={}",
                        i,
                        galleryItem.getTitle(),
                        galleryItem.getDescription() != null ? galleryItem.getDescription().length() : 0,
                        prompt.length());

                String generatedUrl = imageGenerationService.generateImageUrl(prompt);
                if (generatedUrl != null && !generatedUrl.isBlank()) {
                    galleryItem.setImageUrl(generatedUrl);
                    log.info("[Curate-Image] item 이미지 바인딩 완료 - index={}, title={}, url={}",
                            i, galleryItem.getTitle(), generatedUrl);
                } else {
                    log.warn("[Curate-Image] item 이미지 생성 결과 없음 - index={}, title={}",
                            i, galleryItem.getTitle());
                }
            } catch (Exception e) {
                log.warn("[Curate-Image] gallery 이미지 바인딩 실패 - index={}, title={}", i, galleryItem.getTitle(), e);
            }
        }

        log.info("[Curate-Image] gallery 이미지 생성 종료");
    }

    private String buildImagePrompt(SurveyRequestDto survey, GalleryItemDto galleryItem) {
        return """
                Create a warm, shared-memory scene image for a memory gallery.
                Relationship: %s
                Situation: %s
                Tone: %s
                Target age group: %s
                Target name: %s
                Memory title: %s
                Memory description: %s
                Requirements:
                - Depict a real moment they shared together (event/place/time/interaction).
                - Focus on people and atmosphere, not products or gifts.
                - Do NOT depict gift boxes, product shots, rewards, or item catalog style.
                Style: clean, emotionally touching, realistic illustration, no text, no watermark.
                """.formatted(
                nullSafe(survey.getRelationship()),
                nullSafe(survey.getSituation()),
                nullSafe(survey.getTone()),
                nullSafe(survey.getTargetAge()),
                nullSafe(survey.getTargetName()),
                nullSafe(galleryItem.getTitle()),
                nullSafe(galleryItem.getDescription())
        );
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private void limitGalleryToOne(CurateResponseDto result) {
        if (result.getGallery() == null || result.getGallery().isEmpty()) {
            return;
        }
        if (result.getGallery().size() > 1) {
            result.setGallery(List.of(result.getGallery().get(0)));
        }
    }

    private boolean hasAnyGame(ContentDto content) {
        return content != null && (
                (content.getGacha() != null && content.getGacha().getList() != null && !content.getGacha().getList().isEmpty())
                        || (content.getQuiz() != null && content.getQuiz().getList() != null && !content.getQuiz().getList().isEmpty())
                        || content.getUnboxing() != null
        );
    }

    /** LLM이 content를 안 넣었을 때만 쓰는 기본 content (가챌, 퀴즈, 선물 개봉) - 방어 로직 */
    private ContentDto buildExampleContent() {
        GachaDto gacha = GachaDto.builder()
                .playCount(3)
                .list(List.of(
                        GachaItemDto.builder()
                                .itemName("닌텐도 스위치2")
                                .imageUrl("https://example.com/images/switch.png")
                                .percent(0.001)
                                .percentOpen(false)
                                .build()
                ))
                .build();

        QuizDto quiz = QuizDto.builder()
                .successReward(QuizRewardDto.builder()
                        .requiredCount(3)
                        .itemName("특급 한우 세트")
                        .imageUrl("https://example.com/images/beef.png")
                        .build())
                .failReward(QuizRewardDto.builder()
                        .itemName("비타500")
                        .imageUrl("https://example.com/images/vita500.png")
                        .build())
                .list(List.of(
                        QuizItemDto.builder()
                                .quizId(1)
                                .type("multiple_choice")
                                .title("내가 제일 좋아하는 음식은?")
                                .imageUrl("https://example.com/images/food.png")
                                .description("힌트: 매콤한 소스가 특징입니다.")
                                .hint("어제 저녁에도 먹었어요!")
                                .options(List.of("치킨", "마라탕", "초밥", "삼겹살", "파스타"))
                                .answer(List.of("마라탕"))
                                .playLimit(2)
                                .build(),
                        QuizItemDto.builder()
                                .quizId(2)
                                .type("ox")
                                .title("나는 아침형 인간이다.")
                                .imageUrl("https://example.com/images/morning.png")
                                .description("진실 혹은 거짓!")
                                .hint("새벽까지 게임하는 걸 좋아해요.")
                                .options(List.of())
                                .answer(List.of("X"))
                                .playLimit(1)
                                .build()
                ))
                .build();

        UnboxingDto unboxing = UnboxingDto.builder()
                .beforeOpen(UnboxingPhaseDto.builder()
                        .imageUrl("https://example.com/images/gift_box.png")
                        .description("상자 안에 무엇이 들어있을까요? 클릭해서 확인해보세요!")
                        .build())
                .afterOpen(UnboxingAfterDto.builder()
                        .itemName("아이패드 프로 M4")
                        .imageUrl("https://example.com/images/ipad.png")
                        .build())
                .build();

        return ContentDto.builder()
                .gacha(gacha)
                .quiz(quiz)
                .unboxing(unboxing)
                .build();
    }
}
