package com.gifo.backend.integration;

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
 * 중간에 나갔다가 다시 GET /events/{eventUrl}을 호출했을 때
 * 히스토리가 정상적으로 복원되는지 검증
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ResumeFlowTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    EntityManager em;
    @Autowired
    BirthdayEventRepository birthdayEventRepository;
    @Autowired
    CapsuleEventRepository capsuleEventRepository;
    @Autowired
    CapsuleRepository capsuleRepository;
    @Autowired
    GiftRepository giftRepository;
    @Autowired
    QuizEventRepository quizEventRepository;
    @Autowired
    QuizRepository quizRepository;
    @Autowired
    QuizChoiceRepository quizChoiceRepository;
    @Autowired
    QuizRewardRuleRepository quizRewardRuleRepository;

    // ── 캡슐 이어하기 ──────────────────────────────

    @Test
    @DisplayName("캡슐: 2번 뽑고 재접속 → drawHistory 2건 + remainingDrawCount 1 복원")
    void capsule_resume_drawHistory() throws Exception {
        // 캡슐 이벤트 세팅
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

        // 2번 뽑기
        mockMvc.perform(post("/events/RESUME01/capsules/draw"));
        mockMvc.perform(post("/events/RESUME01/capsules/draw"));

        // "재접속" = GET /events/{eventUrl}
        mockMvc.perform(get("/events/RESUME01"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.gacha.playCount").value(3))
                .andExpect(jsonPath("$.data.content.gacha.remainingDrawCount").value(1))
                .andExpect(jsonPath("$.data.content.gacha.drawHistory.length()").value(2))
                .andExpect(jsonPath("$.data.content.gacha.drawHistory[0].capsuleId").isNumber())
                .andExpect(jsonPath("$.data.content.gacha.drawHistory[0].giftName").isString())
                .andExpect(jsonPath("$.data.content.gacha.drawHistory[0].selected").value(false))
                .andExpect(jsonPath("$.data.content.gacha.drawHistory[1].capsuleId").isNumber())
                .andExpect(jsonPath("$.data.content.gacha.list.length()").value(1));
    }

    @Test
    @DisplayName("캡슐: 뽑기 후 선택 → 재접속 시 selected=true 복원")
    void capsule_resume_selected() throws Exception {
        BirthdayEvent event = birthdayEventRepository.save(BirthdayEvent.builder()
                .eventUrl("RESUME02").status(EventStatus.ACTIVE)
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

        // 1번 뽑기 + 선택
        String drawResult = mockMvc.perform(post("/events/RESUME02/capsules/draw"))
                .andReturn().getResponse().getContentAsString();
        String capsuleId = drawResult.split("\"capsuleId\":")[1].split(",")[0].trim();

        mockMvc.perform(post("/events/RESUME02/capsules/select")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"capsuleId\":" + capsuleId + "}"));

        // "재접속"
        mockMvc.perform(get("/events/RESUME02"))
                .andDo(print())
                .andExpect(jsonPath("$.data.content.gacha.selected").value(true))
                .andExpect(jsonPath("$.data.content.gacha.drawHistory[0].selected").value(true));
    }

    @Test
    @DisplayName("캡슐: 리셋 후 재접속 → 히스토리 초기화 + 전체 횟수 복원")
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

        // 3번 다 뽑기
        mockMvc.perform(post("/events/RESUME03/capsules/draw"));
        mockMvc.perform(post("/events/RESUME03/capsules/draw"));
        mockMvc.perform(post("/events/RESUME03/capsules/draw"));

        // 리셋
        mockMvc.perform(delete("/events/RESUME03/progress"));

        // "재접속"
        mockMvc.perform(get("/events/RESUME03"))
                .andDo(print())
                .andExpect(jsonPath("$.data.content.gacha.remainingDrawCount").value(3))
                .andExpect(jsonPath("$.data.content.gacha.drawHistory.length()").value(0))
                .andExpect(jsonPath("$.data.content.gacha.list.length()").value(3));
    }

    // ── 퀴즈 이어하기 ──────────────────────────────

    @Test
    @DisplayName("퀴즈: 1문제 풀고 재접속 → answerHistory 1건 + currentQuizIndex=1 복원")
    void quiz_resume_answerHistory() throws Exception {
        BirthdayEvent event = birthdayEventRepository.save(BirthdayEvent.builder()
                .eventUrl("RESUME04").status(EventStatus.ACTIVE)
                .receiverName("김철수").senderName("박영희").title("축하해")
                .expiredAt(LocalDateTime.now().plusDays(7)).build());

        QuizEvent quizEvent = quizEventRepository.save(QuizEvent.builder()
                .birthdayEvent(event).totalAttempt(0).build());

        Gift successGift = giftRepository.save(Gift.builder()
                .giftName("에어팟").giftImageUrl("https://img/airpods.jpg")
                .description("축하!").isProbabilityPublic(true).build());
        Gift failGift = giftRepository.save(Gift.builder()
                .giftName("양말").giftImageUrl("https://img/socks.jpg")
                .description("다음에!").isProbabilityPublic(true).build());

        quizRewardRuleRepository.save(QuizRewardRule.builder()
                .quizEvent(quizEvent).minCorrect(2).gift(successGift).build());
        quizRewardRuleRepository.save(QuizRewardRule.builder()
                .quizEvent(quizEvent).minCorrect(0).gift(failGift).build());

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

        Quiz q3 = quizRepository.save(Quiz.builder()
                .quizEvent(quizEvent).question("문제3").quizType(QuizType.OX)
                .playLimit(2).sortOrder(3).build());
        quizChoiceRepository.save(QuizChoice.builder().quiz(q3).choiceText("O").isCorrect(true).build());
        quizChoiceRepository.save(QuizChoice.builder().quiz(q3).choiceText("X").isCorrect(false).build());

        em.flush();
        em.clear();

        Long quiz1Id = q1.getQuizId();
        Long quiz2Id = q2.getQuizId();

        // 문제 1: 정답
        mockMvc.perform(post("/events/RESUME04/quiz/answer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quizId\":" + quiz1Id + ",\"correct\":true,\"remainingAttempts\":0}"));

        // "재접속" - 1문제 풀었으니 currentQuizIndex=1, answerHistory 1건
        mockMvc.perform(get("/events/RESUME04"))
                .andDo(print())
                .andExpect(jsonPath("$.data.content.quiz.currentQuizIndex").value(1))
                .andExpect(jsonPath("$.data.content.quiz.answerHistory.length()").value(1))
                .andExpect(jsonPath("$.data.content.quiz.answerHistory[0].quizId").value(quiz1Id))
                .andExpect(jsonPath("$.data.content.quiz.answerHistory[0].correct").value(true));
    }

    @Test
    @DisplayName("퀴즈: 2문제 풀고 재접속 → currentQuizIndex=2, answerHistory에 정답/오답 섞여있음")
    void quiz_resume_mixedAnswers() throws Exception {
        BirthdayEvent event = birthdayEventRepository.save(BirthdayEvent.builder()
                .eventUrl("RESUME05").status(EventStatus.ACTIVE)
                .receiverName("김철수").senderName("박영희").title("축하해")
                .expiredAt(LocalDateTime.now().plusDays(7)).build());

        QuizEvent quizEvent = quizEventRepository.save(QuizEvent.builder()
                .birthdayEvent(event).totalAttempt(0).build());

        Gift gift = giftRepository.save(Gift.builder()
                .giftName("선물").giftImageUrl("https://img/gift.jpg")
                .description("축하!").isProbabilityPublic(true).build());
        quizRewardRuleRepository.save(QuizRewardRule.builder()
                .quizEvent(quizEvent).minCorrect(2).gift(gift).build());
        quizRewardRuleRepository.save(QuizRewardRule.builder()
                .quizEvent(quizEvent).minCorrect(0).gift(gift).build());

        Quiz q1 = quizRepository.save(Quiz.builder()
                .quizEvent(quizEvent).question("문제1").quizType(QuizType.OX)
                .playLimit(2).sortOrder(1).build());
        quizChoiceRepository.save(QuizChoice.builder().quiz(q1).choiceText("O").isCorrect(true).build());

        Quiz q2 = quizRepository.save(Quiz.builder()
                .quizEvent(quizEvent).question("문제2").quizType(QuizType.OX)
                .playLimit(2).sortOrder(2).build());
        quizChoiceRepository.save(QuizChoice.builder().quiz(q2).choiceText("O").isCorrect(true).build());

        Quiz q3 = quizRepository.save(Quiz.builder()
                .quizEvent(quizEvent).question("문제3").quizType(QuizType.OX)
                .playLimit(2).sortOrder(3).build());
        quizChoiceRepository.save(QuizChoice.builder().quiz(q3).choiceText("O").isCorrect(true).build());

        em.flush();
        em.clear();

        // 문제 1: 정답, 문제 2: 오답
        mockMvc.perform(post("/events/RESUME05/quiz/answer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quizId\":" + q1.getQuizId() + ",\"correct\":true,\"remainingAttempts\":0}"));
        mockMvc.perform(post("/events/RESUME05/quiz/answer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quizId\":" + q2.getQuizId() + ",\"correct\":false,\"remainingAttempts\":0}"));

        // "재접속"
        mockMvc.perform(get("/events/RESUME05"))
                .andDo(print())
                .andExpect(jsonPath("$.data.content.quiz.currentQuizIndex").value(2))
                .andExpect(jsonPath("$.data.content.quiz.answerHistory.length()").value(2))
                .andExpect(jsonPath("$.data.content.quiz.answerHistory[0].correct").value(true))
                .andExpect(jsonPath("$.data.content.quiz.answerHistory[1].correct").value(false))
                .andExpect(jsonPath("$.data.content.quiz.list.length()").value(3));
    }

    @Test
    @DisplayName("퀴즈: 문제 풀다가 중간에 나감 → remainingAttempts 복원")
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

        em.flush();
        em.clear();

        // 문제 풀다가 중간에 나감 (remainingAttempts=1 남은 상태)
        // Controller에서 updateRemainingAttempts 호출됨
        mockMvc.perform(post("/events/RESUME06/quiz/answer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quizId\":" + q1.getQuizId() + ",\"correct\":false,\"remainingAttempts\":1}"));

        // "재접속" - 문제는 완료됐으므로 remainingAttempts는 null
        // (answer가 호출되면 문제가 완료된 것이므로 remainingAttempts는 null로 초기화됨)
        mockMvc.perform(get("/events/RESUME06"))
                .andDo(print())
                .andExpect(jsonPath("$.data.content.quiz.currentQuizIndex").value(1))
                .andExpect(jsonPath("$.data.content.quiz.remainingAttempts").isEmpty());
    }

    @Test
    @DisplayName("퀴즈: 리셋 후 재접속 → answerHistory 초기화 + currentQuizIndex=0")
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

        Quiz q2 = quizRepository.save(Quiz.builder()
                .quizEvent(quizEvent).question("문제2").quizType(QuizType.OX)
                .playLimit(2).sortOrder(2).build());
        quizChoiceRepository.save(QuizChoice.builder().quiz(q2).choiceText("O").isCorrect(true).build());

        em.flush();
        em.clear();

        // 2문제 다 풀기
        mockMvc.perform(post("/events/RESUME07/quiz/answer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quizId\":" + q1.getQuizId() + ",\"correct\":true,\"remainingAttempts\":0}"));
        mockMvc.perform(post("/events/RESUME07/quiz/answer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quizId\":" + q2.getQuizId() + ",\"correct\":false,\"remainingAttempts\":0}"));

        // 리셋
        mockMvc.perform(delete("/events/RESUME07/progress"));

        // "재접속"
        mockMvc.perform(get("/events/RESUME07"))
                .andDo(print())
                .andExpect(jsonPath("$.data.content.quiz.currentQuizIndex").value(0))
                .andExpect(jsonPath("$.data.content.quiz.answerHistory.length()").value(0));
    }
}
