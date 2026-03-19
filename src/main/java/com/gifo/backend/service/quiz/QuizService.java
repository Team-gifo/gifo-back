package com.gifo.backend.service.quiz;

import com.gifo.backend.dto.quiz.QuizRequest;
import com.gifo.backend.dto.quiz.QuizResponse;
import com.gifo.backend.entity.event.BirthdayEvent;
import com.gifo.backend.entity.quiz.QuizEvent;
import com.gifo.backend.global.ErrorCode;
import com.gifo.backend.global.exception.quiz.QuizException;
import com.gifo.backend.global.util.EntityFinder;
import com.gifo.backend.repository.quiz.QuizEventRepository;
import com.gifo.backend.repository.quiz.QuizRewardRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 퀴즈 결과 저장 비즈니스 로직
 *
 * POST /events/{eventUrl}/quiz/result 처리:
 * 1. 이벤트 유효성 검증 (ACTIVE 상태인지)
 * 2. correctCount 유효성 검증
 * 3. 원자적 totalAttempt 증가
 * 4. 보상 규칙 기준 성공/실패 판정
 * 5. 결과(lastCorrectCount, lastSuccess) QuizEvent에 영속
 */
@Service
@RequiredArgsConstructor
@Transactional
public class QuizService {

    private final EntityFinder entityFinder;
    private final QuizEventRepository quizEventRepository;
    private final QuizRewardRuleRepository quizRewardRuleRepository;

    public QuizResponse.Result saveResult(String eventUrl, QuizRequest.Result request) {
        BirthdayEvent event = entityFinder.getEventByUrlOrThrow(eventUrl);

        QuizEvent quizEvent = event.getQuizEvent();

        int totalQuizCount = quizEvent.getQuizzes().size();
        if (request.correctCount() < 0 || request.correctCount() > totalQuizCount) {
            throw new QuizException(ErrorCode.INVALID_ARGUMENT);
        }

        // 원자적 totalAttempt 증가 (동시성 안전)
        quizEventRepository.incrementTotalAttempt(quizEvent.getQuizEventId());

        // 보상 규칙으로 성공 여부 판정
        boolean success = quizRewardRuleRepository
                .findByQuizEventOrderByMinCorrectDesc(quizEvent).stream()
                .filter(r -> r.getMinCorrect() != null && r.getMinCorrect() > 0)
                .anyMatch(r -> request.correctCount() >= r.getMinCorrect());

        // 결과를 QuizEvent에 영속
        quizEvent.setLastCorrectCount(request.correctCount());
        quizEvent.setLastSuccess(success);

        return new QuizResponse.Result(request.correctCount(), success);
    }
}
