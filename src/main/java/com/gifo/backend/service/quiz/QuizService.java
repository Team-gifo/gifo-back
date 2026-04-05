package com.gifo.backend.service.quiz;

import com.gifo.backend.dto.quiz.QuizRequest;
import com.gifo.backend.dto.quiz.QuizResponse;
import com.gifo.backend.entity.event.BirthdayEvent;
import com.gifo.backend.entity.quiz.*;
import com.gifo.backend.global.ErrorCode;
import com.gifo.backend.global.exception.quiz.QuizException;
import com.gifo.backend.global.util.EntityFinder;
import com.gifo.backend.repository.quiz.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 퀴즈 비즈니스 로직
 *
 * 1. POST /events/{eventUrl}/quiz/answer — 매 시도마다 호출
 *    서버가 채점 + remainingAttempts 관리
 *    - 정답 → QuizAnswer 저장 + 다음 문제로
 *    - 오답 + 횟수 남음 → remainingAttempts 차감
 *    - 오답 + 횟수 소진 → QuizAnswer(correct=false) 저장 + 다음 문제로
 *
 * 2. POST /events/{eventUrl}/quiz/result — 최종 보상 판정
 *    모든 문제 완료 후 호출 → 서버가 DB에서 정답 수 계산 → 성공/실패 보상 반환
 */
@Service
@RequiredArgsConstructor
@Transactional
public class QuizService {

    private final EntityFinder entityFinder;
    private final QuizRepository quizRepository;
    private final QuizEventRepository quizEventRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final QuizRewardRuleRepository quizRewardRuleRepository;
    private final QuizChoiceRepository quizChoiceRepository;

    /**
     * 답안 제출 (매 시도마다 호출)
     * 서버가 채점하고 remainingAttempts를 관리합니다.
     */
    public QuizResponse.Answer submitAnswer(String eventUrl, QuizRequest.Answer request) {
        BirthdayEvent event = entityFinder.getEventByUrlOrThrow(eventUrl);

        QuizEvent quizEvent = event.getQuizEvent();
        if (quizEvent == null) {
            throw new QuizException(ErrorCode.QUIZ_NOT_FOUND);
        }

        // 해당 문제가 현재 이벤트에 소속되는지 검증
        Quiz quiz = quizRepository.findByQuizEventAndQuizId(quizEvent, request.quizId())
                .orElseThrow(() -> new QuizException(ErrorCode.QUIZ_QUESTION_NOT_FOUND));

        // 이미 답변 완료된 문제인지 검증
        if (quizAnswerRepository.existsByQuizEventAndQuiz(quizEvent, quiz)) {
            throw new QuizException(ErrorCode.QUIZ_ALREADY_ANSWERED);
        }

        // remainingAttempts 초기화 (첫 시도 시 playLimit으로 세팅)
        Integer remaining = quizEvent.getCurrentQuizRemainingAttempts();
        if (remaining == null) {
            remaining = quiz.getPlayLimit();
        }

        // 시도 횟수 소진 검증
        if (remaining <= 0) {
            throw new QuizException(ErrorCode.QUIZ_NO_ATTEMPTS_LEFT);
        }

        // 서버 채점: selectedAnswer와 DB 정답 비교
        boolean correct = gradeAnswer(quiz, request.selectedAnswer());

        if (correct) {
            // 정답 → QuizAnswer 저장 + remainingAttempts 초기화
            saveQuizAnswer(quizEvent, quiz, true);
            quizEvent.setCurrentQuizRemainingAttempts(null);

            int currentQuizIndex = (int) quizAnswerRepository.countByQuizEvent(quizEvent);
            return new QuizResponse.Answer(request.quizId(), true, 0, currentQuizIndex);
        } else {
            // 오답 → 횟수 차감
            remaining -= 1;

            if (remaining <= 0) {
                // 횟수 소진 → QuizAnswer(correct=false) 저장 + remainingAttempts 초기화
                saveQuizAnswer(quizEvent, quiz, false);
                quizEvent.setCurrentQuizRemainingAttempts(null);

                int currentQuizIndex = (int) quizAnswerRepository.countByQuizEvent(quizEvent);
                return new QuizResponse.Answer(request.quizId(), false, 0, currentQuizIndex);
            } else {
                // 아직 기회 남음 → remainingAttempts만 업데이트
                quizEvent.setCurrentQuizRemainingAttempts(remaining);

                int currentQuizIndex = (int) quizAnswerRepository.countByQuizEvent(quizEvent);
                return new QuizResponse.Answer(request.quizId(), false, remaining, currentQuizIndex);
            }
        }
    }

    /**
     * 서버 채점: 퀴즈 타입별 정답 비교
     * - OBJECTIVE / OX: isCorrect=true인 QuizChoice의 choiceText와 비교
     * - SUBJECTIVE: 모든 QuizChoice(허용 답안)와 대소문자 무시 + trim 비교
     */
    private boolean gradeAnswer(Quiz quiz, String selectedAnswer) {
        String trimmed = selectedAnswer.trim();
        List<QuizChoice> choices = quizChoiceRepository.findByQuiz(quiz);

        return switch (quiz.getQuizType()) {
            case OBJECTIVE, OX -> choices.stream()
                    .filter(QuizChoice::getIsCorrect)
                    .anyMatch(c -> c.getChoiceText().trim().equalsIgnoreCase(trimmed));
            case SUBJECTIVE -> choices.stream()
                    .anyMatch(c -> c.getChoiceText().trim().equalsIgnoreCase(trimmed));
        };
    }

    /**
     * QuizAnswer 저장 (unique constraint 위반 시 비즈니스 예외로 변환)
     */
    private void saveQuizAnswer(QuizEvent quizEvent, Quiz quiz, boolean correct) {
        try {
            quizAnswerRepository.saveAndFlush(QuizAnswer.builder()
                    .quizEvent(quizEvent)
                    .quiz(quiz)
                    .correct(correct)
                    .build());
        } catch (DataIntegrityViolationException e) {
            throw new QuizException(ErrorCode.QUIZ_ALREADY_ANSWERED);
        }
    }

    /**
     * 최종 보상 판정
     * 모든 문제 완료 후 서버가 DB에서 정답 수를 계산하여 보상 결정
     */
    public QuizResponse.Result saveResult(String eventUrl, QuizRequest.Result request) {
        BirthdayEvent event = entityFinder.getEventByUrlOrThrow(eventUrl);

        QuizEvent quizEvent = event.getQuizEvent();
        if (quizEvent == null) {
            throw new QuizException(ErrorCode.QUIZ_NOT_FOUND);
        }

        // 모든 문제를 풀었는지 검증
        long answeredCount = quizAnswerRepository.countByQuizEvent(quizEvent);
        long totalQuizCount = quizRepository.countByQuizEvent(quizEvent);
        if (answeredCount < totalQuizCount) {
            throw new QuizException(ErrorCode.QUIZ_NOT_ALL_ANSWERED);
        }

        // 서버가 DB에서 정답 수 계산
        int correctCount = (int) quizAnswerRepository.countByQuizEventAndCorrectTrue(quizEvent);

        // 원자적 totalAttempt 증가 (동시성 안전)
        quizEventRepository.incrementTotalAttempt(quizEvent.getQuizEventId());

        // 보상 규칙으로 성공 여부 판정
        boolean success = quizRewardRuleRepository
                .findByQuizEventOrderByMinCorrectDesc(quizEvent).stream()
                .filter(r -> r.getMinCorrect() != null && r.getMinCorrect() > 0)
                .anyMatch(r -> correctCount >= r.getMinCorrect());

        // 결과를 QuizEvent에 영속
        quizEvent.setLastCorrectCount(correctCount);
        quizEvent.setLastSuccess(success);

        return new QuizResponse.Result(correctCount, success);
    }
}
