package com.gifo.backend.curate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gifo.backend.curate.dto.CurateResponseDto;
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

    public CurateResponseDto curate(SurveyRequestDto survey) {
        String userMessage = promptLoader.loadPrompt(survey);

        try {
            String raw = chatClient.prompt()
                    .user(userMessage)
                    .call()
                    .content();

            if (raw == null || raw.isBlank()) {
                throw new CustomException(ErrorCode.AI_CURATE_ERROR, "AI 응답이 비어 있습니다.");
            }

            String json = stripMarkdownJson(raw);
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
            return result;
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.warn("AI 큐레이션 생성 실패: {}", e.getMessage());
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
