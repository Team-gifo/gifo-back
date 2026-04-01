package com.gifo.backend.integration;

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
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 이어하기(재접속) 플로우 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ResumeFlowTest {

    @Autowired MockMvc mockMvc;
    @Autowired EntityManager em;

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired BirthdayEventRepository birthdayEventRepository;
    @Autowired CapsuleEventRepository capsuleEventRepository;
    @Autowired CapsuleRepository capsuleRepository;
    @Autowired GiftRepository giftRepository;
    @Autowired QuizEventRepository quizEventRepository;
    @Autowired QuizRepository quizRepository;
    @Autowired QuizChoiceRepository quizChoiceRepository;
    @Autowired QuizRewardRuleRepository quizRewardRuleRepository;

    // ── 캡슐 이어하기 ──────────────────────────────

    @Test
    @DisplayName("캡슐: 2번 뽑고 재접속 → drawHistory 2건 + remainingDrawCount 1 복원")
    void capsule_resume_drawHistory() throws Exception {
        BirthdayEvent event = birthdayEventRepository.save(BirthdayEvent.builder()
                .eventUrl("RESUME01").status(EventStatus.ACTIVE)
                .receiverName("김철수").senderName("박영희").title("축하해")
                .expiredAt(LocalDateTime.now().plusDays(7)).build());

        CapsuleEvent capsuleEvent = capsuleEventRepository.save(CapsuleEvent.builder()
                .birthdayEvent(event).maxDrawCount(3).build());

        for (int i = 0; i < 3; i++) {
            Gift gift = giftRepository.save(Gift.builder()
                    .giftName("선물" + i).giftImageUrl("https://img/" + i + ".jpg")
                    .description("설명" + i).isProbabilityPublic(true).build());
            capsuleRepository.save(Capsule.builder()
                    .capsuleEvent(capsuleEvent).gift(gift).weight(10).build());
        }
        em.flush();
        em.clear();

        mockMvc.perform(post("/events/RESUME01/capsules/draw"));
        mockMvc.perform(post("/events/RESUME01/capsules/draw"));

        mockMvc.perform(get("/events/RESUME01"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.gacha.playCount").value(3))
                .andExpect(jsonPath("$.data.content.gacha.remainingDrawCount").value(1))
                .andExpect(jsonPath("$.data.content.gacha.drawHistory.length()").value(2))
                .andExpect(jsonPath("$.data.content.gacha.drawHistory[0].selected").value(false))
                .andExpect(jsonPath("$.data.content.gacha.list.length()").value(1));
    }

    @Test
    @DisplayName("캡슐: 선택 후에도 추가 뽑기 가능")
    void capsule_drawAfterSelect() throws Exception {
        BirthdayEvent event = birthdayEventRepository.save(BirthdayEvent.builder()
                .eventUrl("RESUME09").status(EventStatus.ACTIVE)
                .receiverName("김철수").senderName("박영희").title("축하해")
                .expiredAt(LocalDateTime.now().plusDays(7)).build());

        CapsuleEvent capsuleEvent = capsuleEventRepository.save(CapsuleEvent.builder()
                .birthdayEvent(event).maxDrawCount(3).build());

        for (int i = 0; i < 3; i++) {
            Gift gift = giftRepository.save(Gift.builder()
                    .giftName("선물" + i).giftImageUrl("https://img/" + i + ".jpg")
                    .description("설명" + i).isProbabilityPublic(true).build());
            capsuleRepository.save(Capsule.builder()
                    .capsuleEvent(capsuleEvent).gift(gift).weight(10).build());
        }
        em.flush();
        em.clear();

        String drawResult = mockMvc.perform(post("/events/RESUME09/capsules/draw"))
                .andReturn().getResponse().getContentAsString();
        Long capsuleId = objectMapper.readTree(drawResult).path("data").path("capsuleId").asLong();

        mockMvc.perform(post("/events/RESUME09/capsules/select")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"capsuleId\":" + capsuleId + "}"));

        // 선택 후에도 추가 뽑기 가능
        mockMvc.perform(post("/events/RESUME09/capsules/draw"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.capsuleId").isNumber());

        mockMvc.perform(get("/events/RESUME09"))
                .andExpect(jsonPath("$.data.content.gacha.remainingDrawCount").value(1))
                .andExpect(jsonPath("$.data.content.gacha.drawHistory.length()").value(2));
    }

    @Test
    @DisplayName("캡슐: 리셋 후 재접속 → 히스토리 초기화")
    void capsule_resume_afterReset() throws Exception {
        BirthdayEvent event = birthdayEventRepository.save(BirthdayEvent.builder()
                .eventUrl("RESUME03").status(EventStatus.ACTIVE)
                .receiverName("김철수").senderName("박영희").title("축하해")
                .expiredAt(LocalDateTime.now().plusDays(7)).build());

        CapsuleEvent capsuleEvent = capsuleEventRepository.save(CapsuleEvent.builder()
                .birthdayEvent(event).maxDrawCount(3).build());

        for (int i = 0; i < 3; i++) {
            Gift gift = giftRepository.save(Gift.builder()
                    .giftName("선물" + i).giftImageUrl("https://img/" + i + ".jpg")
                    .description("설명" + i).isProbabilityPublic(true).build());
            capsuleRepository.save(Capsule.builder()
                    .capsuleEvent(capsuleEvent).gift(gift).weight(10).build());
        }
        em.flush();
        em.clear();

        mockMvc.perform(post("/events/RESUME03/capsules/draw"));
        mockMvc.perform(post("/events/RESUME03/capsules/draw"));
        mockMvc.perform(post("/events/RESUME03/capsules/draw"));

        mockMvc.perform(delete("/events/RESUME03/progress"));

        mockMvc.perform(get("/events/RESUME03"))
                .andDo(print())
                .andExpect(jsonPath("$.data.content.gacha.remainingDrawCount").value(3))
                .andExpect(jsonPath("$.data.content.gacha.drawHistory.length()").value(0))
                .andExpect(jsonPath("$.data.content.gacha.list.length()").value(3));
    }

    // ── 퀴즈 이어하기 ──────────────────────────────

    @Test
    @DisplayName("퀴즈: 1문제 정답 후 재접속 → answerHistory 1건 + currentQuizIndex=1")
    void quiz_resume_answerHistory() throws Exception {
        BirthdayEvent event = birthdayEventRepository.save(BirthdayEvent.builder()
                .eventUrl("RESUME04").status(EventStatus.ACTIVE)
                .receiverName("김철수").senderName("박영희").title("축하해")
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
                .playLimit(2).sortOrder(1).build());
        quizChoiceRepository.save(QuizChoice.builder().quiz(q1).choiceText("O").isCorrect(true).build());
        quizChoiceRepository.save(QuizChoice.builder().quiz(q1).choiceText("X").isCorrect(false).build());

        Quiz q2 = quizRepository.save(Quiz.builder()
                .quizEvent(quizEvent).question("문제2").quizType(QuizType.OX)
                .playLimit(2).sortOrder(2).build());
        quizChoiceRepository.save(QuizChoice.builder().quiz(q2).choiceText("O").isCorrect(true).build());
        quizChoiceRepository.save(QuizChoice.builder().quiz(q2).choiceText("X").isCorrect(false).build());

        em.flush();
        em.clear();

        mockMvc.perform(post("/events/RESUME04/quiz/answer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quizId\":" + q1.getQuizId() + ",\"selectedAnswer\":\"O\"}"));

        mockMvc.perform(get("/events/RESUME04"))
                .andDo(print())
                .andExpect(jsonPath("$.data.content.quiz.currentQuizIndex").value(1))
                .andExpect(jsonPath("$.data.content.quiz.answerHistory.length()").value(1))
                .andExpect(jsonPath("$.data.content.quiz.answerHistory[0].correct").value(true));
    }

    @Test
    @DisplayName("퀴즈: 오답 시도 중 재접속 → remainingAttempts 복원")
    void quiz_resume_remainingAttempts() throws Exception {
        BirthdayEvent event = birthdayEventRepository.save(BirthdayEvent.builder()
                .eventUrl("RESUME06").status(EventStatus.ACTIVE)
                .receiverName("김철수").senderName("박영희").title("축하해")
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

        // 1번 오답 → remainingAttempts=2
        mockMvc.perform(post("/events/RESUME06/quiz/answer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quizId\":" + q1.getQuizId() + ",\"selectedAnswer\":\"X\"}"));

        mockMvc.perform(get("/events/RESUME06"))
                .andDo(print())
                .andExpect(jsonPath("$.data.content.quiz.currentQuizIndex").value(0))
                .andExpect(jsonPath("$.data.content.quiz.remainingAttempts").value(2));
    }

    @Test
    @DisplayName("퀴즈: 리셋 후 재접속 → 초기화")
    void quiz_resume_afterReset() throws Exception {
        BirthdayEvent event = birthdayEventRepository.save(BirthdayEvent.builder()
                .eventUrl("RESUME07").status(EventStatus.ACTIVE)
                .receiverName("김철수").senderName("박영희").title("축하해")
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
                .playLimit(2).sortOrder(1).build());
        quizChoiceRepository.save(QuizChoice.builder().quiz(q1).choiceText("O").isCorrect(true).build());
        quizChoiceRepository.save(QuizChoice.builder().quiz(q1).choiceText("X").isCorrect(false).build());

        em.flush();
        em.clear();

        mockMvc.perform(post("/events/RESUME07/quiz/answer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quizId\":" + q1.getQuizId() + ",\"selectedAnswer\":\"O\"}"));

        mockMvc.perform(delete("/events/RESUME07/progress"));

        mockMvc.perform(get("/events/RESUME07"))
                .andDo(print())
                .andExpect(jsonPath("$.data.content.quiz.currentQuizIndex").value(0))
                .andExpect(jsonPath("$.data.content.quiz.answerHistory.length()").value(0));
    }
}
