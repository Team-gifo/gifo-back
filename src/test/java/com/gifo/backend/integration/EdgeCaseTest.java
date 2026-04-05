package com.gifo.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gifo.backend.entity.capsule.Capsule;
import com.gifo.backend.entity.capsule.CapsuleEvent;
import com.gifo.backend.entity.event.BirthdayEvent;
import com.gifo.backend.entity.event.EventStatus;
import com.gifo.backend.entity.gift.Gift;
import com.gifo.backend.entity.quiz.*;
import com.gifo.backend.repository.capsule.CapsuleEventRepository;
import com.gifo.backend.repository.capsule.CapsuleRepository;
import com.gifo.backend.repository.event.BirthdayEventRepository;
import com.gifo.backend.repository.gift.GiftRepository;
import com.gifo.backend.repository.quiz.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 엣지 케이스 통합 테스트
 *
 * 캡슐/퀴즈/이벤트 전반의 경계 조건 및 에러 시나리오를 검증합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EdgeCaseTest {

    @Autowired MockMvc mockMvc;
    @Autowired EntityManager em;
    @Autowired BirthdayEventRepository birthdayEventRepository;
    @Autowired CapsuleEventRepository capsuleEventRepository;
    @Autowired CapsuleRepository capsuleRepository;
    @Autowired GiftRepository giftRepository;
    @Autowired QuizEventRepository quizEventRepository;
    @Autowired QuizRepository quizRepository;
    @Autowired QuizChoiceRepository quizChoiceRepository;
    @Autowired QuizRewardRuleRepository quizRewardRuleRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ══════════════════════════════════════════════
    // 1. CAPSULE_ALL_DRAWN — maxDrawCount > 캡슐 개수
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("캡슐: maxDrawCount > 캡슐 개수일 때 모든 캡슐 소진 → CAPSULE_ALL_DRAWN")
    void capsule_allDrawn() throws Exception {
        BirthdayEvent event = birthdayEventRepository.save(BirthdayEvent.builder()
                .eventUrl("EDGE_CAP1").status(EventStatus.ACTIVE)
                .receiverName("김철수").senderName("박영희").title("생일 축하해")
                .expiredAt(LocalDateTime.now().plusDays(7)).build());

        // 캡슐 2개인데 maxDrawCount=5
        CapsuleEvent capsuleEvent = capsuleEventRepository.save(CapsuleEvent.builder()
                .birthdayEvent(event).maxDrawCount(5).build());

        for (int i = 0; i < 2; i++) {
            Gift gift = giftRepository.save(Gift.builder()
                    .giftName("선물" + i).giftImageUrl("https://img/" + i + ".jpg")
                    .description("설명").isProbabilityPublic(true).build());
            capsuleRepository.save(Capsule.builder()
                    .capsuleEvent(capsuleEvent).gift(gift).weight(50).build());
        }
        em.flush();
        em.clear();

        // 2번 뽑기 (정상)
        mockMvc.perform(post("/events/EDGE_CAP1/capsules/draw"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/events/EDGE_CAP1/capsules/draw"))
                .andExpect(status().isOk());

        // 3번째 뽑기 → 캡슐 소진
        mockMvc.perform(post("/events/EDGE_CAP1/capsules/draw"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CAPSULE_ALL_DRAWN"));
    }

    // ══════════════════════════════════════════════
    // 2. 뽑지 않은 캡슐 select → CAPSULE_DRAW_NOT_FOUND
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("캡슐: 뽑지 않은 capsuleId로 select → CAPSULE_DRAW_NOT_FOUND")
    void capsule_selectNotDrawn() throws Exception {
        BirthdayEvent event = birthdayEventRepository.save(BirthdayEvent.builder()
                .eventUrl("EDGE_CAP2").status(EventStatus.ACTIVE)
                .receiverName("김철수").senderName("박영희").title("생일 축하해")
                .expiredAt(LocalDateTime.now().plusDays(7)).build());

        CapsuleEvent capsuleEvent = capsuleEventRepository.save(CapsuleEvent.builder()
                .birthdayEvent(event).maxDrawCount(3).build());

        Gift gift = giftRepository.save(Gift.builder()
                .giftName("선물").giftImageUrl("https://img/gift.jpg")
                .description("설명").isProbabilityPublic(true).build());
        Capsule capsule = capsuleRepository.save(Capsule.builder()
                .capsuleEvent(capsuleEvent).gift(gift).weight(100).build());
        em.flush();
        em.clear();

        // 뽑지 않고 바로 select 시도
        mockMvc.perform(post("/events/EDGE_CAP2/capsules/select")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"capsuleId\":" + capsule.getCapsuleId() + "}"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CAPSULE_DRAW_NOT_FOUND"));
    }

    // ══════════════════════════════════════════════
    // 3. 잘못된 quizId 제출 → QUIZ_QUESTION_NOT_FOUND
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("퀴즈: 이벤트에 속하지 않는 quizId로 answer 제출 → QUIZ_QUESTION_NOT_FOUND")
    void quiz_invalidQuizId() throws Exception {
        BirthdayEvent event = birthdayEventRepository.save(BirthdayEvent.builder()
                .eventUrl("EDGE_QZ1").status(EventStatus.ACTIVE)
                .receiverName("김철수").senderName("박영희").title("생일 축하해")
                .expiredAt(LocalDateTime.now().plusDays(7)).build());

        QuizEvent quizEvent = quizEventRepository.save(QuizEvent.builder()
                .birthdayEvent(event).totalAttempt(0).build());

        Quiz q1 = quizRepository.save(Quiz.builder()
                .quizEvent(quizEvent).question("문제").quizType(QuizType.OX)
                .playLimit(3).sortOrder(1).build());
        quizChoiceRepository.save(QuizChoice.builder().quiz(q1).choiceText("O").isCorrect(true).build());
        em.flush();
        em.clear();

        // 존재하지 않는 quizId = 99999
        mockMvc.perform(post("/events/EDGE_QZ1/quiz/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":99999,\"selectedAnswer\":\"O\"}"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("QUIZ_QUESTION_NOT_FOUND"));
    }

    // ══════════════════════════════════════════════
    // 4. 만료/삭제된 이벤트 접근
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("이벤트: EXPIRED 상태 이벤트 조회 → EVENT_EXPIRED")
    void event_expired() throws Exception {
        birthdayEventRepository.save(BirthdayEvent.builder()
                .eventUrl("EDGE_EXP").status(EventStatus.EXPIRED)
                .receiverName("김철수").senderName("박영희").title("생일 축하해")
                .expiredAt(LocalDateTime.now().minusDays(1)).build());
        em.flush();
        em.clear();

        mockMvc.perform(get("/events/EDGE_EXP"))
                .andDo(print())
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("EVENT_EXPIRED"));
    }

    @Test
    @DisplayName("이벤트: DELETED 상태 이벤트 조회 → EVENT_DELETED")
    void event_deleted() throws Exception {
        birthdayEventRepository.save(BirthdayEvent.builder()
                .eventUrl("EDGE_DEL").status(EventStatus.DELETED)
                .receiverName("김철수").senderName("박영희").title("생일 축하해")
                .expiredAt(LocalDateTime.now().plusDays(7)).build());
        em.flush();
        em.clear();

        mockMvc.perform(get("/events/EDGE_DEL"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EVENT_DELETED"));
    }

    @Test
    @DisplayName("이벤트: EXPIRED 이벤트에 캡슐 뽑기 시도 → EVENT_EXPIRED")
    void event_expired_capsuleDraw() throws Exception {
        BirthdayEvent event = birthdayEventRepository.save(BirthdayEvent.builder()
                .eventUrl("EDGE_EXP2").status(EventStatus.EXPIRED)
                .receiverName("김철수").senderName("박영희").title("생일 축하해")
                .expiredAt(LocalDateTime.now().minusDays(1)).build());

        CapsuleEvent capsuleEvent = capsuleEventRepository.save(CapsuleEvent.builder()
                .birthdayEvent(event).maxDrawCount(3).build());
        em.flush();
        em.clear();

        mockMvc.perform(post("/events/EDGE_EXP2/capsules/draw"))
                .andDo(print())
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("EVENT_EXPIRED"));
    }

    @Test
    @DisplayName("이벤트: EXPIRED 이벤트에 퀴즈 답안 제출 → EVENT_EXPIRED")
    void event_expired_quizAnswer() throws Exception {
        BirthdayEvent event = birthdayEventRepository.save(BirthdayEvent.builder()
                .eventUrl("EDGE_EXP3").status(EventStatus.EXPIRED)
                .receiverName("김철수").senderName("박영희").title("생일 축하해")
                .expiredAt(LocalDateTime.now().minusDays(1)).build());

        QuizEvent quizEvent = quizEventRepository.save(QuizEvent.builder()
                .birthdayEvent(event).totalAttempt(0).build());
        em.flush();
        em.clear();

        mockMvc.perform(post("/events/EDGE_EXP3/quiz/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":1,\"selectedAnswer\":\"O\"}"))
                .andDo(print())
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("EVENT_EXPIRED"));
    }

    // ══════════════════════════════════════════════
    // 5. 퀴즈 리셋 후 재시도
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("퀴즈: 리셋 후 같은 문제 다시 풀기 가능")
    void quiz_resetAndRetry() throws Exception {
        BirthdayEvent event = birthdayEventRepository.save(BirthdayEvent.builder()
                .eventUrl("EDGE_RST1").status(EventStatus.ACTIVE)
                .receiverName("김철수").senderName("박영희").title("생일 축하해")
                .expiredAt(LocalDateTime.now().plusDays(7)).build());

        QuizEvent quizEvent = quizEventRepository.save(QuizEvent.builder()
                .birthdayEvent(event).totalAttempt(0).build());

        Gift gift = giftRepository.save(Gift.builder()
                .giftName("선물").giftImageUrl("https://img/gift.jpg")
                .description("축하!").isProbabilityPublic(true).build());
        quizRewardRuleRepository.save(QuizRewardRule.builder()
                .quizEvent(quizEvent).minCorrect(1).gift(gift).build());
        quizRewardRuleRepository.save(QuizRewardRule.builder()
                .quizEvent(quizEvent).minCorrect(0).gift(gift).build());

        Quiz q1 = quizRepository.save(Quiz.builder()
                .quizEvent(quizEvent).question("문제1").quizType(QuizType.OX)
                .playLimit(3).sortOrder(1).build());
        quizChoiceRepository.save(QuizChoice.builder().quiz(q1).choiceText("O").isCorrect(true).build());
        quizChoiceRepository.save(QuizChoice.builder().quiz(q1).choiceText("X").isCorrect(false).build());
        em.flush();
        em.clear();

        // 1차: 오답 3번으로 실패
        mockMvc.perform(post("/events/EDGE_RST1/quiz/answer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quizId\":" + q1.getQuizId() + ",\"selectedAnswer\":\"X\"}"))
                .andExpect(jsonPath("$.data.remainingAttempts").value(2));
        mockMvc.perform(post("/events/EDGE_RST1/quiz/answer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quizId\":" + q1.getQuizId() + ",\"selectedAnswer\":\"X\"}"))
                .andExpect(jsonPath("$.data.remainingAttempts").value(1));
        mockMvc.perform(post("/events/EDGE_RST1/quiz/answer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quizId\":" + q1.getQuizId() + ",\"selectedAnswer\":\"X\"}"))
                .andExpect(jsonPath("$.data.correct").value(false))
                .andExpect(jsonPath("$.data.remainingAttempts").value(0));

        // result 호출
        mockMvc.perform(post("/events/EDGE_RST1/quiz/result"))
                .andExpect(jsonPath("$.data.correctCount").value(0))
                .andExpect(jsonPath("$.data.success").value(false));

        // 리셋
        mockMvc.perform(delete("/events/EDGE_RST1/progress"))
                .andExpect(status().isOk());

        // 리셋 후 초기 상태 확인
        mockMvc.perform(get("/events/EDGE_RST1"))
                .andExpect(jsonPath("$.data.content.quiz.currentQuizIndex").value(0))
                .andExpect(jsonPath("$.data.content.quiz.answerHistory.length()").value(0))
                .andExpect(jsonPath("$.data.content.quiz.remainingAttempts").isEmpty());

        // 2차: 같은 문제 다시 풀기 → 정답
        mockMvc.perform(post("/events/EDGE_RST1/quiz/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":" + q1.getQuizId() + ",\"selectedAnswer\":\"O\"}"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correct").value(true))
                .andExpect(jsonPath("$.data.currentQuizIndex").value(1));

        // 2차 result → 성공
        mockMvc.perform(post("/events/EDGE_RST1/quiz/result"))
                .andExpect(jsonPath("$.data.correctCount").value(1))
                .andExpect(jsonPath("$.data.success").value(true));
    }

    // ══════════════════════════════════════════════
    // 6. 공백/빈 문자열 답안
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("퀴즈: 공백만 있는 답안 제출 → @NotBlank VALIDATION_ERROR")
    void quiz_whitespaceAnswer() throws Exception {
        BirthdayEvent event = birthdayEventRepository.save(BirthdayEvent.builder()
                .eventUrl("EDGE_WS").status(EventStatus.ACTIVE)
                .receiverName("김철수").senderName("박영희").title("생일 축하해")
                .expiredAt(LocalDateTime.now().plusDays(7)).build());

        QuizEvent quizEvent = quizEventRepository.save(QuizEvent.builder()
                .birthdayEvent(event).totalAttempt(0).build());

        Quiz q1 = quizRepository.save(Quiz.builder()
                .quizEvent(quizEvent).question("문제").quizType(QuizType.SUBJECTIVE)
                .playLimit(3).sortOrder(1).build());
        quizChoiceRepository.save(QuizChoice.builder().quiz(q1).choiceText("정답").isCorrect(true).build());
        em.flush();
        em.clear();

        // 공백만 입력 → @NotBlank에 의해 validation 실패
        mockMvc.perform(post("/events/EDGE_WS/quiz/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":" + q1.getQuizId() + ",\"selectedAnswer\":\"   \"}"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // ══════════════════════════════════════════════
    // 7. OX 대소문자 무시
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("퀴즈: OX 소문자 'o' 입력도 정답 처리")
    void quiz_ox_caseInsensitive() throws Exception {
        BirthdayEvent event = birthdayEventRepository.save(BirthdayEvent.builder()
                .eventUrl("EDGE_OX").status(EventStatus.ACTIVE)
                .receiverName("김철수").senderName("박영희").title("생일 축하해")
                .expiredAt(LocalDateTime.now().plusDays(7)).build());

        QuizEvent quizEvent = quizEventRepository.save(QuizEvent.builder()
                .birthdayEvent(event).totalAttempt(0).build());

        Quiz q1 = quizRepository.save(Quiz.builder()
                .quizEvent(quizEvent).question("고양이 키운다").quizType(QuizType.OX)
                .playLimit(3).sortOrder(1).build());
        quizChoiceRepository.save(QuizChoice.builder().quiz(q1).choiceText("O").isCorrect(true).build());
        quizChoiceRepository.save(QuizChoice.builder().quiz(q1).choiceText("X").isCorrect(false).build());
        em.flush();
        em.clear();

        // 소문자 'o'로 제출
        mockMvc.perform(post("/events/EDGE_OX/quiz/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":" + q1.getQuizId() + ",\"selectedAnswer\":\"o\"}"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correct").value(true));
    }

    @Test
    @DisplayName("퀴즈: OX 소문자 'x'도 오답으로 인식 (정답이 O일 때)")
    void quiz_ox_lowercase_x() throws Exception {
        BirthdayEvent event = birthdayEventRepository.save(BirthdayEvent.builder()
                .eventUrl("EDGE_OX2").status(EventStatus.ACTIVE)
                .receiverName("김철수").senderName("박영희").title("생일 축하해")
                .expiredAt(LocalDateTime.now().plusDays(7)).build());

        QuizEvent quizEvent = quizEventRepository.save(QuizEvent.builder()
                .birthdayEvent(event).totalAttempt(0).build());

        Quiz q1 = quizRepository.save(Quiz.builder()
                .quizEvent(quizEvent).question("고양이 키운다").quizType(QuizType.OX)
                .playLimit(3).sortOrder(1).build());
        quizChoiceRepository.save(QuizChoice.builder().quiz(q1).choiceText("O").isCorrect(true).build());
        quizChoiceRepository.save(QuizChoice.builder().quiz(q1).choiceText("X").isCorrect(false).build());
        em.flush();
        em.clear();

        // 소문자 'x'로 제출 → 오답
        mockMvc.perform(post("/events/EDGE_OX2/quiz/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":" + q1.getQuizId() + ",\"selectedAnswer\":\"x\"}"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correct").value(false));
    }

    // ══════════════════════════════════════════════
    // 8. 요청 유효성 검증 (@Valid)
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("퀴즈: quizId 누락 시 에러 응답")
    void quiz_missingQuizId() throws Exception {
        BirthdayEvent event = birthdayEventRepository.save(BirthdayEvent.builder()
                .eventUrl("EDGE_VAL1").status(EventStatus.ACTIVE)
                .receiverName("김철수").senderName("박영희").title("생일 축하해")
                .expiredAt(LocalDateTime.now().plusDays(7)).build());
        quizEventRepository.save(QuizEvent.builder()
                .birthdayEvent(event).totalAttempt(0).build());
        em.flush();
        em.clear();

        // quizId 누락 → @NotNull VALIDATION_ERROR
        mockMvc.perform(post("/events/EDGE_VAL1/quiz/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"selectedAnswer\":\"O\"}"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("퀴즈: selectedAnswer 빈 문자열 → @NotBlank VALIDATION_ERROR")
    void quiz_emptySelectedAnswer() throws Exception {
        BirthdayEvent event = birthdayEventRepository.save(BirthdayEvent.builder()
                .eventUrl("EDGE_VAL2").status(EventStatus.ACTIVE)
                .receiverName("김철수").senderName("박영희").title("생일 축하해")
                .expiredAt(LocalDateTime.now().plusDays(7)).build());
        QuizEvent quizEvent = quizEventRepository.save(QuizEvent.builder()
                .birthdayEvent(event).totalAttempt(0).build());
        Quiz q1 = quizRepository.save(Quiz.builder()
                .quizEvent(quizEvent).question("문제").quizType(QuizType.OX)
                .playLimit(3).sortOrder(1).build());
        quizChoiceRepository.save(QuizChoice.builder().quiz(q1).choiceText("O").isCorrect(true).build());
        em.flush();
        em.clear();

        mockMvc.perform(post("/events/EDGE_VAL2/quiz/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":" + q1.getQuizId() + ",\"selectedAnswer\":\"\"}"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("캡슐: capsuleId null로 select → @NotNull VALIDATION_ERROR")
    void capsule_nullCapsuleId() throws Exception {
        BirthdayEvent event = birthdayEventRepository.save(BirthdayEvent.builder()
                .eventUrl("EDGE_VAL3").status(EventStatus.ACTIVE)
                .receiverName("김철수").senderName("박영희").title("생일 축하해")
                .expiredAt(LocalDateTime.now().plusDays(7)).build());
        capsuleEventRepository.save(CapsuleEvent.builder()
                .birthdayEvent(event).maxDrawCount(3).build());
        em.flush();
        em.clear();

        mockMvc.perform(post("/events/EDGE_VAL3/capsules/select")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"capsuleId\":null}"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // ══════════════════════════════════════════════
    // 9. 캡슐 리셋 후 재뽑기 전체 플로우
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("캡슐: 리셋 후 다시 전체 횟수만큼 뽑기 가능")
    void capsule_resetAndRedraw() throws Exception {
        BirthdayEvent event = birthdayEventRepository.save(BirthdayEvent.builder()
                .eventUrl("EDGE_RST2").status(EventStatus.ACTIVE)
                .receiverName("김철수").senderName("박영희").title("생일 축하해")
                .expiredAt(LocalDateTime.now().plusDays(7)).build());

        CapsuleEvent capsuleEvent = capsuleEventRepository.save(CapsuleEvent.builder()
                .birthdayEvent(event).maxDrawCount(2).build());

        for (int i = 0; i < 3; i++) {
            Gift gift = giftRepository.save(Gift.builder()
                    .giftName("선물" + i).giftImageUrl("https://img/" + i + ".jpg")
                    .description("설명").isProbabilityPublic(true).build());
            capsuleRepository.save(Capsule.builder()
                    .capsuleEvent(capsuleEvent).gift(gift).weight(30).build());
        }
        em.flush();
        em.clear();

        // 2번 뽑기
        mockMvc.perform(post("/events/EDGE_RST2/capsules/draw"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/events/EDGE_RST2/capsules/draw"))
                .andExpect(status().isOk());

        // 횟수 초과
        mockMvc.perform(post("/events/EDGE_RST2/capsules/draw"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CAPSULE_DRAW_LIMIT_EXCEEDED"));

        // 리셋
        mockMvc.perform(delete("/events/EDGE_RST2/progress"))
                .andExpect(status().isOk());

        // 리셋 후 초기 상태 확인
        mockMvc.perform(get("/events/EDGE_RST2"))
                .andExpect(jsonPath("$.data.content.gacha.remainingDrawCount").value(2))
                .andExpect(jsonPath("$.data.content.gacha.drawHistory.length()").value(0))
                .andExpect(jsonPath("$.data.content.gacha.selected").value(false));

        // 다시 2번 뽑기 가능
        mockMvc.perform(post("/events/EDGE_RST2/capsules/draw"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/events/EDGE_RST2/capsules/draw"))
                .andDo(print())
                .andExpect(status().isOk());

        // 다시 횟수 초과
        mockMvc.perform(post("/events/EDGE_RST2/capsules/draw"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CAPSULE_DRAW_LIMIT_EXCEEDED"));
    }

    // ══════════════════════════════════════════════
    // 10. 존재하지 않는 eventUrl 접근
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("이벤트: 존재하지 않는 eventUrl 조회 → EVENT_NOT_FOUND")
    void event_notFound() throws Exception {
        mockMvc.perform(get("/events/NOTEXIST"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EVENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("이벤트: 존재하지 않는 eventUrl로 캡슐 뽑기 → EVENT_NOT_FOUND")
    void event_notFound_capsuleDraw() throws Exception {
        mockMvc.perform(post("/events/NOTEXIST/capsules/draw"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EVENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("이벤트: 존재하지 않는 eventUrl로 퀴즈 답안 제출 → EVENT_NOT_FOUND")
    void event_notFound_quizAnswer() throws Exception {
        mockMvc.perform(post("/events/NOTEXIST/quiz/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":1,\"selectedAnswer\":\"O\"}"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EVENT_NOT_FOUND"));
    }

    // ══════════════════════════════════════════════
    // 11. 객관식 대소문자 무시
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("퀴즈: 객관식 대소문자 무시 비교 (영어 답안)")
    void quiz_objective_caseInsensitive() throws Exception {
        BirthdayEvent event = birthdayEventRepository.save(BirthdayEvent.builder()
                .eventUrl("EDGE_CASE").status(EventStatus.ACTIVE)
                .receiverName("김철수").senderName("박영희").title("생일 축하해")
                .expiredAt(LocalDateTime.now().plusDays(7)).build());

        QuizEvent quizEvent = quizEventRepository.save(QuizEvent.builder()
                .birthdayEvent(event).totalAttempt(0).build());

        Quiz q1 = quizRepository.save(Quiz.builder()
                .quizEvent(quizEvent).question("좋아하는 브랜드?").quizType(QuizType.OBJECTIVE)
                .playLimit(3).sortOrder(1).build());
        quizChoiceRepository.save(QuizChoice.builder().quiz(q1).choiceText("Apple").isCorrect(true).build());
        quizChoiceRepository.save(QuizChoice.builder().quiz(q1).choiceText("Samsung").isCorrect(false).build());
        em.flush();
        em.clear();

        // 소문자 "apple"로 제출 → 정답
        mockMvc.perform(post("/events/EDGE_CASE/quiz/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":" + q1.getQuizId() + ",\"selectedAnswer\":\"apple\"}"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correct").value(true));
    }

    // ══════════════════════════════════════════════
    // 12. 답안 앞뒤 공백 trim 처리
    // ══════════════════════════════════════════════

    @Test
    @DisplayName("퀴즈: 답안 앞뒤 공백 trim 후 비교")
    void quiz_trimAnswer() throws Exception {
        BirthdayEvent event = birthdayEventRepository.save(BirthdayEvent.builder()
                .eventUrl("EDGE_TRIM").status(EventStatus.ACTIVE)
                .receiverName("김철수").senderName("박영희").title("생일 축하해")
                .expiredAt(LocalDateTime.now().plusDays(7)).build());

        QuizEvent quizEvent = quizEventRepository.save(QuizEvent.builder()
                .birthdayEvent(event).totalAttempt(0).build());

        Quiz q1 = quizRepository.save(Quiz.builder()
                .quizEvent(quizEvent).question("처음 만난 장소?").quizType(QuizType.SUBJECTIVE)
                .playLimit(3).sortOrder(1).build());
        quizChoiceRepository.save(QuizChoice.builder().quiz(q1).choiceText("스타벅스").isCorrect(true).build());
        em.flush();
        em.clear();

        // 앞뒤 공백 포함 "  스타벅스  " → 정답
        mockMvc.perform(post("/events/EDGE_TRIM/quiz/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":" + q1.getQuizId() + ",\"selectedAnswer\":\"  스타벅스  \"}"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correct").value(true));
    }
}
