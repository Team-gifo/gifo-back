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

/**
 * 퀴즈 전체 플로우 시나리오 테스트
 *
 * 3문제, 각 3번씩 기회 (playLimit=3)
 * Q1(객관식): 3번 모두 오답 → 시도 소진, correct=false
 * Q2(OX):    1번 오답 → 2번째에 정답
 * Q3(주관식): 첫 시도에 정답
 * → result: correctCount=2, success=true (minCorrect=2)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class QuizFullFlowTest {

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
                .eventUrl("FLOW1234").status(EventStatus.ACTIVE)
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

        // 2개 이상 정답이면 성공
        quizRewardRuleRepository.save(QuizRewardRule.builder()
                .quizEvent(quizEvent).minCorrect(2).gift(successGift).build());
        quizRewardRuleRepository.save(QuizRewardRule.builder()
                .quizEvent(quizEvent).minCorrect(0).gift(failGift).build());

        // Q1 객관식 (정답: "치킨"), playLimit=3
        Quiz q1 = quizRepository.save(Quiz.builder()
                .quizEvent(quizEvent).question("좋아하는 음식은?")
                .quizType(QuizType.OBJECTIVE).playLimit(3).sortOrder(1).build());
        quiz1Id = q1.getQuizId();
        quizChoiceRepository.save(QuizChoice.builder().quiz(q1).choiceText("치킨").isCorrect(true).build());
        quizChoiceRepository.save(QuizChoice.builder().quiz(q1).choiceText("피자").isCorrect(false).build());
        quizChoiceRepository.save(QuizChoice.builder().quiz(q1).choiceText("햄버거").isCorrect(false).build());

        // Q2 OX (정답: "O"), playLimit=3
        Quiz q2 = quizRepository.save(Quiz.builder()
                .quizEvent(quizEvent).question("고양이를 키운다")
                .quizType(QuizType.OX).playLimit(3).sortOrder(2).build());
        quiz2Id = q2.getQuizId();
        quizChoiceRepository.save(QuizChoice.builder().quiz(q2).choiceText("O").isCorrect(true).build());
        quizChoiceRepository.save(QuizChoice.builder().quiz(q2).choiceText("X").isCorrect(false).build());

        // Q3 주관식 (허용 답안: "스타벅스", "스벅"), playLimit=3
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
    @DisplayName("전체 시나리오: Q1 3번 오답 → Q2 1번 오답+정답 → Q3 첫 시도 정답 → result 성공")
    void fullFlow_mixedResults() throws Exception {
        // ── Q1: 객관식, 3번 모두 오답 (시도 소진) ──

        // 1번째 시도: 오답 → remainingAttempts=2
        mockMvc.perform(post("/events/{eventUrl}/quiz/answer", eventUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":" + quiz1Id + ",\"selectedAnswer\":\"피자\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correct").value(false))
                .andExpect(jsonPath("$.data.remainingAttempts").value(2))
                .andExpect(jsonPath("$.data.currentQuizIndex").value(0));

        // 2번째 시도: 오답 → remainingAttempts=1
        mockMvc.perform(post("/events/{eventUrl}/quiz/answer", eventUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":" + quiz1Id + ",\"selectedAnswer\":\"햄버거\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correct").value(false))
                .andExpect(jsonPath("$.data.remainingAttempts").value(1))
                .andExpect(jsonPath("$.data.currentQuizIndex").value(0));

        // 3번째 시도: 오답 → remainingAttempts=0, 시도 소진으로 다음 문제
        mockMvc.perform(post("/events/{eventUrl}/quiz/answer", eventUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":" + quiz1Id + ",\"selectedAnswer\":\"피자\"}"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correct").value(false))
                .andExpect(jsonPath("$.data.remainingAttempts").value(0))
                .andExpect(jsonPath("$.data.currentQuizIndex").value(1));

        // Q1 다시 제출하면 QUIZ_ALREADY_ANSWERED
        mockMvc.perform(post("/events/{eventUrl}/quiz/answer", eventUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":" + quiz1Id + ",\"selectedAnswer\":\"치킨\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("QUIZ_ALREADY_ANSWERED"));

        // ── Q2: OX, 1번 오답 후 2번째에 정답 ──

        // 1번째 시도: 오답 → remainingAttempts=2
        mockMvc.perform(post("/events/{eventUrl}/quiz/answer", eventUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":" + quiz2Id + ",\"selectedAnswer\":\"X\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correct").value(false))
                .andExpect(jsonPath("$.data.remainingAttempts").value(2))
                .andExpect(jsonPath("$.data.currentQuizIndex").value(1));

        // 2번째 시도: 정답 → remainingAttempts=0, 다음 문제
        mockMvc.perform(post("/events/{eventUrl}/quiz/answer", eventUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":" + quiz2Id + ",\"selectedAnswer\":\"O\"}"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correct").value(true))
                .andExpect(jsonPath("$.data.remainingAttempts").value(0))
                .andExpect(jsonPath("$.data.currentQuizIndex").value(2));

        // ── Q3: 주관식, 첫 시도에 정답 ──

        mockMvc.perform(post("/events/{eventUrl}/quiz/answer", eventUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quizId\":" + quiz3Id + ",\"selectedAnswer\":\"스벅\"}"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correct").value(true))
                .andExpect(jsonPath("$.data.remainingAttempts").value(0))
                .andExpect(jsonPath("$.data.currentQuizIndex").value(3));

        // ── GET 재접속: answerHistory 3개, currentQuizIndex=3 ──

        mockMvc.perform(get("/events/{eventUrl}", eventUrl))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.quiz.currentQuizIndex").value(3))
                .andExpect(jsonPath("$.data.content.quiz.answerHistory.length()").value(3))
                .andExpect(jsonPath("$.data.content.quiz.list[0].answer").doesNotExist())
                .andExpect(jsonPath("$.data.content.quiz.list[1].answer").doesNotExist())
                .andExpect(jsonPath("$.data.content.quiz.list[2].answer").doesNotExist())
                .andExpect(jsonPath("$.data.content.quiz.answerHistory[0].correct").value(false))
                .andExpect(jsonPath("$.data.content.quiz.answerHistory[1].correct").value(true))
                .andExpect(jsonPath("$.data.content.quiz.answerHistory[2].correct").value(true));

        // ── result: correctCount=2 (Q2+Q3), success=true (minCorrect=2) ──

        // 서버가 DB에서 정답 수를 직접 계산하므로 body 없이 호출
        mockMvc.perform(post("/events/{eventUrl}/quiz/result", eventUrl))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correctCount").value(2))
                .andExpect(jsonPath("$.data.success").value(true));
    }
}
