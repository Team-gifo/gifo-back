package com.gifo.backend.service.quiz;

import com.gifo.backend.dto.quiz.QuizRequest;
import com.gifo.backend.dto.quiz.QuizResponse;
import com.gifo.backend.entity.event.BirthdayEvent;
import com.gifo.backend.entity.quiz.Quiz;
import com.gifo.backend.entity.quiz.QuizAnswer;
import com.gifo.backend.entity.quiz.QuizEvent;
import com.gifo.backend.global.ErrorCode;
import com.gifo.backend.global.exception.quiz.QuizException;
import com.gifo.backend.global.util.EntityFinder;
import com.gifo.backend.repository.quiz.QuizAnswerRepository;
import com.gifo.backend.repository.quiz.QuizEventRepository;
import com.gifo.backend.repository.quiz.QuizRepository;
import com.gifo.backend.repository.quiz.QuizRewardRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 퀴즈 비즈니스 로직
 *
 * 1. POST /events/{eventUrl}/quiz/answer — 문제별 결과 저장
 *    정답을 맞추거나 playLimit 소진 시 호출 → QuizAnswer 저장 + remainingAttempts 관리
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

    /**
     * 문제별 결과 저장
     * 프론트에서 정답을 맞추거나 playLimit을 소진했을 때 호출
     */
    public QuizResponse.Answer saveAnswer(String eventUrl, QuizRequest.Answer request) {
        BirthdayEvent event = entityFinder.getEventByUrlOrThrow(eventUrl);

        QuizEvent quizEvent = event.getQuizEvent();
        if (quizEvent == null) {
            throw new QuizException(ErrorCode.QUIZ_NOT_FOUND);
        }

        // 해당 문제 존재 검증
        Quiz quiz = quizRepository.findById(request.quizId())
                .orElseThrow(() -> new QuizException(ErrorCode.QUIZ_QUESTION_NOT_FOUND));

        // 이미 답변한 문제인지 검증
        if (quizAnswerRepository.existsByQuizEventAndQuiz(quizEvent, quiz)) {
            throw new QuizException(ErrorCode.QUIZ_ALREADY_ANSWERED);
        }

        // QuizAnswer 저장
        quizAnswerRepository.save(QuizAnswer.builder()
                .quizEvent(quizEvent)
                .quiz(quiz)
                .correct(request.correct())
                .build());

        // 현재 문제의 남은 시도 횟수 저장 (재접속 시 이어하기용)
        // 문제가 완료되었으므로 null로 초기화 (다음 문제는 아직 시작 안 함)
        quizEvent.setCurrentQuizRemainingAttempts(null);

        int currentQuizIndex = (int) quizAnswerRepository.countByQuizEvent(quizEvent);

        return new QuizResponse.Answer(request.quizId(), request.correct(), currentQuizIndex);
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

    /**
     * 현재 풀고 있는 문제의 남은 시도 횟수 업데이트
     * 프론트에서 오답 시도할 때마다 호출하여 재접속 시 이어하기용으로 저장
     */
    public void updateRemainingAttempts(String eventUrl, int remainingAttempts) {
        if (remainingAttempts < 0) {
            throw new QuizException(ErrorCode.INVALID_ARGUMENT);
        }

        BirthdayEvent event = entityFinder.getEventByUrlOrThrow(eventUrl);

        QuizEvent quizEvent = event.getQuizEvent();
        if (quizEvent == null) {
            throw new QuizException(ErrorCode.QUIZ_NOT_FOUND);
        }

        quizEvent.setCurrentQuizRemainingAttempts(remainingAttempts);
    }
}
