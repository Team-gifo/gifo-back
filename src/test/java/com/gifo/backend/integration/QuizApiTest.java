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

        // 객관식: 정답 "치킨"
        Quiz q1 = quizRepository.save(Quiz.builder()
                .quizEvent(quizEvent).question("좋아하는 음식은?")
                .quizType(QuizType.OBJECTIVE).playLimit(3).sortOrder(1).build());
        quiz1Id = q1.getQuizId();
        quizChoiceRepository.save(QuizChoice.builder().quiz(q1).choiceText("치킨").isCorrect(true).build());
        quizChoiceRepository.save(QuizChoice.builder().quiz(q1).choiceText("피자").isCorrect(false).build());

        // OX: 정답 "O"
        Quiz q2 = quizRepository.save(Quiz.builder()
                .quizEvent(quizEvent).question("고양이를 키운다")
                .quizType(QuizType.OX).playLimit(2).sortOrder(2).build());
        quiz2Id = q2.getQuizId();
        quizChoiceRepository.save(QuizChoice.builder().quiz(q2).choiceText("O").isCorrect(true).build());
        quizChoiceRepository.save(QuizChoice.builder().quiz(q2).choiceText("X").isCorrect(false).build());

        // 주관식: 허용 답안 "스타벅스", "스벅"
        Quiz q3 = quizRepository.save(Quiz.builder()
                .quizEvent(quizEvent).question("처음 만난 장소?")
                .quizType(QuizType.SUBJECTIVE).playLimit(3).sortOrder(3).build());
        quiz3Id = q3.getQuizId();
        quizChoiceRepository.save(QuizChoice.builder().quiz(q3).choiceText("스타벅스").isCorrect(true).build());
        quizChoiceRepository.save(QuizChoice.builder().quiz(q3).choiceText("스벅").isCorrect(true).build());

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("1. GET /events/{eventUrl} - 퀴즈 이벤트 초기 조회 (answer 필드 없음)")
    void getEvent_quiz_initial() throws Exception {
        mockMvc.perform(get("/events/{eventUrl}", eventUrl))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.quiz.currentQuizIndex").value(0))
                .andExpect(jsonPath("$.data.content.quiz.remainingAttempts").isEmpty())
                .andExpect(jsonPath("$.data.content.quiz.list.length()").value(3))
                .andExpect(jsonPath("$.data.content.quiz.list[0].options.length()").value(2))
                .andExpect(jsonPath("$.data.content.quiz.list[0].answer").doesNotExist())
                .andExpect(jsonPath("$.data.content.quiz.answerHistory.length()").value(0));
    }

    @Test
    @DisplayName("2. POST /quiz/answer - 정답 제출 → correct=true, remainingAttempts=0")
    void submitAnswer_correct() throws Exception {
        mockMvc.perform(post("/events/{eventUrl}/quiz/answer", eventUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":" + quiz1Id + ",\"selectedAnswer\":\"치킨\"}"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quizId").value(quiz1Id))
                .andExpect(jsonPath("$.data.correct").value(true))
                .andExpect(jsonPath("$.data.remainingAttempts").value(0))
                .andExpect(jsonPath("$.data.currentQuizIndex").value(1));
    }

    @Test
    @DisplayName("3. POST /quiz/answer - 오답 제출 → correct=false, remainingAttempts 차감")
    void submitAnswer_incorrect_hasRetry() throws Exception {
        mockMvc.perform(post("/events/{eventUrl}/quiz/answer", eventUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":" + quiz1Id + ",\"selectedAnswer\":\"피자\"}"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correct").value(false))
                .andExpect(jsonPath("$.data.remainingAttempts").value(2))
                .andExpect(jsonPath("$.data.currentQuizIndex").value(0));
    }

    @Test
    @DisplayName("4. 오답 3번으로 playLimit 소진 → correct=false, remainingAttempts=0, 다음 문제로")
    void submitAnswer_exhaustAttempts() throws Exception {
        mockMvc.perform(post("/events/{eventUrl}/quiz/answer", eventUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quizId\":" + quiz1Id + ",\"selectedAnswer\":\"피자\"}"))
                .andExpect(jsonPath("$.data.remainingAttempts").value(2));

        mockMvc.perform(post("/events/{eventUrl}/quiz/answer", eventUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quizId\":" + quiz1Id + ",\"selectedAnswer\":\"피자\"}"))
                .andExpect(jsonPath("$.data.remainingAttempts").value(1));

        mockMvc.perform(post("/events/{eventUrl}/quiz/answer", eventUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":" + quiz1Id + ",\"selectedAnswer\":\"피자\"}"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correct").value(false))
                .andExpect(jsonPath("$.data.remainingAttempts").value(0))
                .andExpect(jsonPath("$.data.currentQuizIndex").value(1));
    }

    @Test
    @DisplayName("5. 이미 답변 완료된 문제에 다시 제출 → QUIZ_ALREADY_ANSWERED")
    void submitAnswer_duplicate() throws Exception {
        mockMvc.perform(post("/events/{eventUrl}/quiz/answer", eventUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quizId\":" + quiz1Id + ",\"selectedAnswer\":\"치킨\"}"));

        mockMvc.perform(post("/events/{eventUrl}/quiz/answer", eventUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":" + quiz1Id + ",\"selectedAnswer\":\"치킨\"}"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("QUIZ_ALREADY_ANSWERED"));
    }

    @Test
    @DisplayName("6. 전체 플로우: 3문제 정답 → result → 성공 보상")
    void fullFlow_success() throws Exception {
        mockMvc.perform(post("/events/{eventUrl}/quiz/answer", eventUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quizId\":" + quiz1Id + ",\"selectedAnswer\":\"치킨\"}"));
        mockMvc.perform(post("/events/{eventUrl}/quiz/answer", eventUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quizId\":" + quiz2Id + ",\"selectedAnswer\":\"O\"}"));
        mockMvc.perform(post("/events/{eventUrl}/quiz/answer", eventUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quizId\":" + quiz3Id + ",\"selectedAnswer\":\"스벅\"}"));

        mockMvc.perform(get("/events/{eventUrl}", eventUrl))
                .andExpect(jsonPath("$.data.content.quiz.currentQuizIndex").value(3))
                .andExpect(jsonPath("$.data.content.quiz.answerHistory.length()").value(3));

        mockMvc.perform(post("/events/{eventUrl}/quiz/result", eventUrl))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correctCount").value(3))
                .andExpect(jsonPath("$.data.success").value(true));
    }

    @Test
    @DisplayName("7. 전체 플로우: 1개만 정답 → result → 실패 보상")
    void fullFlow_fail() throws Exception {
        mockMvc.perform(post("/events/{eventUrl}/quiz/answer", eventUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quizId\":" + quiz1Id + ",\"selectedAnswer\":\"치킨\"}"));
        // playLimit 소진 (2번 오답)
        mockMvc.perform(post("/events/{eventUrl}/quiz/answer", eventUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quizId\":" + quiz2Id + ",\"selectedAnswer\":\"X\"}"));
        mockMvc.perform(post("/events/{eventUrl}/quiz/answer", eventUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quizId\":" + quiz2Id + ",\"selectedAnswer\":\"X\"}"));
        // playLimit 소진 (3번 오답)
        mockMvc.perform(post("/events/{eventUrl}/quiz/answer", eventUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quizId\":" + quiz3Id + ",\"selectedAnswer\":\"맥도날드\"}"));
        mockMvc.perform(post("/events/{eventUrl}/quiz/answer", eventUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quizId\":" + quiz3Id + ",\"selectedAnswer\":\"투썸\"}"));
        mockMvc.perform(post("/events/{eventUrl}/quiz/answer", eventUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quizId\":" + quiz3Id + ",\"selectedAnswer\":\"이디야\"}"));

        mockMvc.perform(post("/events/{eventUrl}/quiz/result", eventUrl))
                .andDo(print())
                .andExpect(jsonPath("$.data.correctCount").value(1))
                .andExpect(jsonPath("$.data.success").value(false));
    }

    @Test
    @DisplayName("8. 주관식 대소문자 무시 비교")
    void submitAnswer_subjective_caseInsensitive() throws Exception {
        mockMvc.perform(post("/events/{eventUrl}/quiz/answer", eventUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quizId\":" + quiz1Id + ",\"selectedAnswer\":\"치킨\"}"));
        mockMvc.perform(post("/events/{eventUrl}/quiz/answer", eventUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quizId\":" + quiz2Id + ",\"selectedAnswer\":\"O\"}"));

        mockMvc.perform(post("/events/{eventUrl}/quiz/answer", eventUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":" + quiz3Id + ",\"selectedAnswer\":\"스타벅스\"}"))
                .andDo(print())
                .andExpect(jsonPath("$.data.correct").value(true));
    }

    @Test
    @DisplayName("9. 모든 문제 풀기 전 result 호출 시 에러")
    void result_beforeAllAnswered() throws Exception {
        mockMvc.perform(post("/events/{eventUrl}/quiz/answer", eventUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quizId\":" + quiz1Id + ",\"selectedAnswer\":\"치킨\"}"));

        mockMvc.perform(post("/events/{eventUrl}/quiz/result", eventUrl))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("QUIZ_NOT_ALL_ANSWERED"));
    }

    @Test
    @DisplayName("10. DELETE /progress - 퀴즈 리셋 후 초기화 확인")
    void resetProgress_quiz() throws Exception {
        mockMvc.perform(post("/events/{eventUrl}/quiz/answer", eventUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quizId\":" + quiz1Id + ",\"selectedAnswer\":\"치킨\"}"));

        mockMvc.perform(delete("/events/{eventUrl}/progress", eventUrl))
                .andExpect(status().isOk());

        mockMvc.perform(get("/events/{eventUrl}", eventUrl))
                .andDo(print())
                .andExpect(jsonPath("$.data.content.quiz.currentQuizIndex").value(0))
                .andExpect(jsonPath("$.data.content.quiz.answerHistory.length()").value(0));
    }

    @Test
    @DisplayName("11. 오답 후 재접속 → remainingAttempts 복원")
    void remainingAttempts_restoredOnResume() throws Exception {
        mockMvc.perform(post("/events/{eventUrl}/quiz/answer", eventUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quizId\":" + quiz1Id + ",\"selectedAnswer\":\"피자\"}"));

        mockMvc.perform(get("/events/{eventUrl}", eventUrl))
                .andDo(print())
                .andExpect(jsonPath("$.data.content.quiz.currentQuizIndex").value(0))
                .andExpect(jsonPath("$.data.content.quiz.remainingAttempts").value(2));
    }
}
