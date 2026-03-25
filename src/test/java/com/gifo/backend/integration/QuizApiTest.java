package com.gifo.backend.integration;

import com.gifo.backend.entity.event.BirthdayEvent;
import com.gifo.backend.entity.event.EventStatus;
import com.gifo.backend.entity.gift.Gift;
import com.gifo.backend.entity.quiz.*;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class QuizApiTest {

    @Autowired MockMvc mockMvc;
    @Autowired EntityManager em;
    @Autowired BirthdayEventRepository birthdayEventRepository;
    @Autowired QuizEventRepository quizEventRepository;
    @Autowired QuizRepository quizRepository;
    @Autowired QuizChoiceRepository quizChoiceRepository;
    @Autowired QuizRewardRuleRepository quizRewardRuleRepository;
    @Autowired GiftRepository giftRepository;

    private String eventUrl;
    private Long quiz1Id;
    private Long quiz2Id;
    private Long quiz3Id;

    @BeforeEach
    void setUp() {
        BirthdayEvent event = birthdayEventRepository.save(BirthdayEvent.builder()
                .eventUrl("QUIZ1234").status(EventStatus.ACTIVE)
                .receiverName("김철수").senderName("박영희").title("생일 축하해")
                .expiredAt(LocalDateTime.now().plusDays(7)).build());
        eventUrl = event.getEventUrl();

        QuizEvent quizEvent = quizEventRepository.save(QuizEvent.builder()
                .birthdayEvent(event).totalAttempt(0).build());

        Gift successGift = giftRepository.save(Gift.builder()
                .giftName("에어팟 프로").giftImageUrl("https://img/airpods.jpg")
                .description("축하해요!").isProbabilityPublic(true).build());
        Gift failGift = giftRepository.save(Gift.builder()
                .giftName("양말 세트").giftImageUrl("https://img/socks.jpg")
                .description("다음에!").isProbabilityPublic(true).build());

        quizRewardRuleRepository.save(QuizRewardRule.builder()
                .quizEvent(quizEvent).minCorrect(2).gift(successGift).build());
        quizRewardRuleRepository.save(QuizRewardRule.builder()
                .quizEvent(quizEvent).minCorrect(0).gift(failGift).build());

        Quiz q1 = quizRepository.save(Quiz.builder()
                .quizEvent(quizEvent).question("좋아하는 음식은?")
                .quizType(QuizType.OBJECTIVE).playLimit(3).sortOrder(1).build());
        quiz1Id = q1.getQuizId();
        quizChoiceRepository.save(QuizChoice.builder().quiz(q1).choiceText("치킨").isCorrect(true).build());
        quizChoiceRepository.save(QuizChoice.builder().quiz(q1).choiceText("피자").isCorrect(false).build());

        Quiz q2 = quizRepository.save(Quiz.builder()
                .quizEvent(quizEvent).question("고양이를 키운다")
                .quizType(QuizType.OX).playLimit(2).sortOrder(2).build());
        quiz2Id = q2.getQuizId();
        quizChoiceRepository.save(QuizChoice.builder().quiz(q2).choiceText("O").isCorrect(true).build());
        quizChoiceRepository.save(QuizChoice.builder().quiz(q2).choiceText("X").isCorrect(false).build());

        Quiz q3 = quizRepository.save(Quiz.builder()
                .quizEvent(quizEvent).question("처음 만난 장소?")
                .quizType(QuizType.SUBJECTIVE).playLimit(3).sortOrder(3).build());
        quiz3Id = q3.getQuizId();
        quizChoiceRepository.save(QuizChoice.builder().quiz(q3).choiceText("스타벅스").isCorrect(true).build());

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("1. GET /events/{eventUrl} - 퀴즈 이벤트 초기 조회")
    void getEvent_quiz_initial() throws Exception {
        mockMvc.perform(get("/events/{eventUrl}", eventUrl))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.gacha").isEmpty())
                .andExpect(jsonPath("$.data.content.quiz.currentQuizIndex").value(0))
                .andExpect(jsonPath("$.data.content.quiz.remainingAttempts").isEmpty())
                .andExpect(jsonPath("$.data.content.quiz.successReward.requiredCount").value(2))
                .andExpect(jsonPath("$.data.content.quiz.successReward.itemName").value("에어팟 프로"))
                .andExpect(jsonPath("$.data.content.quiz.failReward.itemName").value("양말 세트"))
                .andExpect(jsonPath("$.data.content.quiz.list.length()").value(3))
                .andExpect(jsonPath("$.data.content.quiz.answerHistory.length()").value(0));
    }

    @Test
    @DisplayName("2. POST /quiz/answer - 정답 저장")
    void saveAnswer_correct() throws Exception {
        mockMvc.perform(post("/events/{eventUrl}/quiz/answer", eventUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":" + quiz1Id + ",\"correct\":true,\"remainingAttempts\":0}"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.quizId").value(quiz1Id))
                .andExpect(jsonPath("$.data.correct").value(true))
                .andExpect(jsonPath("$.data.currentQuizIndex").value(1));
    }

    @Test
    @DisplayName("3. POST /quiz/answer - 오답 저장")
    void saveAnswer_incorrect() throws Exception {
        mockMvc.perform(post("/events/{eventUrl}/quiz/answer", eventUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":" + quiz1Id + ",\"correct\":false,\"remainingAttempts\":0}"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correct").value(false))
                .andExpect(jsonPath("$.data.currentQuizIndex").value(1));
    }

    @Test
    @DisplayName("4. 이미 답변한 문제 다시 저장 시 에러")
    void saveAnswer_duplicate() throws Exception {
        mockMvc.perform(post("/events/{eventUrl}/quiz/answer", eventUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quizId\":" + quiz1Id + ",\"correct\":true,\"remainingAttempts\":0}"));

        mockMvc.perform(post("/events/{eventUrl}/quiz/answer", eventUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":" + quiz1Id + ",\"correct\":true,\"remainingAttempts\":0}"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("QUIZ_ALREADY_ANSWERED"));
    }

    @Test
    @DisplayName("5. 전체 플로우: 3문제 풀기 → result → 성공 보상 (2개 이상 정답)")
    void fullFlow_success() throws Exception {
        mockMvc.perform(post("/events/{eventUrl}/quiz/answer", eventUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quizId\":" + quiz1Id + ",\"correct\":true,\"remainingAttempts\":0}"));
        mockMvc.perform(post("/events/{eventUrl}/quiz/answer", eventUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quizId\":" + quiz2Id + ",\"correct\":true,\"remainingAttempts\":0}"));
        mockMvc.perform(post("/events/{eventUrl}/quiz/answer", eventUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quizId\":" + quiz3Id + ",\"correct\":false,\"remainingAttempts\":0}"));

        mockMvc.perform(get("/events/{eventUrl}", eventUrl))
                .andExpect(jsonPath("$.data.content.quiz.currentQuizIndex").value(3))
                .andExpect(jsonPath("$.data.content.quiz.answerHistory.length()").value(3))
                .andExpect(jsonPath("$.data.content.quiz.answerHistory[0].correct").value(true))
                .andExpect(jsonPath("$.data.content.quiz.answerHistory[2].correct").value(false));

        mockMvc.perform(post("/events/{eventUrl}/quiz/result", eventUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correctCount\":2}"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correctCount").value(2))
                .andExpect(jsonPath("$.data.success").value(true));
    }

    @Test
    @DisplayName("6. 전체 플로우: 3문제 풀기 → result → 실패 보상 (1개 정답)")
    void fullFlow_fail() throws Exception {
        mockMvc.perform(post("/events/{eventUrl}/quiz/answer", eventUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quizId\":" + quiz1Id + ",\"correct\":true,\"remainingAttempts\":0}"));
        mockMvc.perform(post("/events/{eventUrl}/quiz/answer", eventUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quizId\":" + quiz2Id + ",\"correct\":false,\"remainingAttempts\":0}"));
        mockMvc.perform(post("/events/{eventUrl}/quiz/answer", eventUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quizId\":" + quiz3Id + ",\"correct\":false,\"remainingAttempts\":0}"));

        mockMvc.perform(post("/events/{eventUrl}/quiz/result", eventUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correctCount\":1}"))
                .andDo(print())
                .andExpect(jsonPath("$.data.correctCount").value(1))
                .andExpect(jsonPath("$.data.success").value(false));
    }

    @Test
    @DisplayName("7. 답변 완료 후 remainingAttempts null 초기화 확인")
    void remainingAttempts_clearedAfterAnswer() throws Exception {
        mockMvc.perform(post("/events/{eventUrl}/quiz/answer", eventUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quizId\":" + quiz1Id + ",\"correct\":true,\"remainingAttempts\":2}"));

        mockMvc.perform(get("/events/{eventUrl}", eventUrl))
                .andDo(print())
                .andExpect(jsonPath("$.data.content.quiz.currentQuizIndex").value(1))
                .andExpect(jsonPath("$.data.content.quiz.remainingAttempts").isEmpty());
    }

    @Test
    @DisplayName("9. 모든 문제 풀기 전 result 호출 시 에러")
    void result_beforeAllAnswered() throws Exception {
        mockMvc.perform(post("/events/{eventUrl}/quiz/answer", eventUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quizId\":" + quiz1Id + ",\"correct\":true,\"remainingAttempts\":0}"));

        mockMvc.perform(post("/events/{eventUrl}/quiz/result", eventUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correctCount\":1}"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("QUIZ_NOT_ALL_ANSWERED"));
    }

    @Test
    @DisplayName("8. DELETE /progress - 퀴즈 리셋 후 초기화 확인")
    void resetProgress_quiz() throws Exception {
        mockMvc.perform(post("/events/{eventUrl}/quiz/answer", eventUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quizId\":" + quiz1Id + ",\"correct\":true,\"remainingAttempts\":0}"));
        mockMvc.perform(post("/events/{eventUrl}/quiz/answer", eventUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quizId\":" + quiz2Id + ",\"correct\":false,\"remainingAttempts\":0}"));

        mockMvc.perform(delete("/events/{eventUrl}/progress", eventUrl))
                .andExpect(status().isOk());

        mockMvc.perform(get("/events/{eventUrl}", eventUrl))
                .andDo(print())
                .andExpect(jsonPath("$.data.content.quiz.currentQuizIndex").value(0))
                .andExpect(jsonPath("$.data.content.quiz.answerHistory.length()").value(0));
    }
}
