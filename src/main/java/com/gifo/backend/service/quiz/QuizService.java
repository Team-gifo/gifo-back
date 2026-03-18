package com.gifo.backend.service.quiz;

import com.gifo.backend.dto.quiz.QuizRequest;
import com.gifo.backend.dto.quiz.QuizResponse;
import com.gifo.backend.entity.event.BirthdayEvent;
import com.gifo.backend.entity.gift.Gift;
import com.gifo.backend.entity.quiz.*;
import com.gifo.backend.global.ErrorCode;
import com.gifo.backend.global.exception.quiz.QuizException;
import com.gifo.backend.global.util.EntityFinder;
import com.gifo.backend.repository.quiz.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 퀴즈 제출 비즈니스 로직
 *
 * POST /events/{eventUrl}/quiz/submit 처리:
 * 1. 이벤트 유효성 검증 (ACTIVE 상태인지)
 * 2. 문항별 정답 검증
 *    - OBJECTIVE(객관식): 선택한 값이 is_correct=true 인 선택지와 정확히 일치하는지
 *    - OX: 위와 동일
 *    - SUBJECTIVE(주관식): 저장된 정답 텍스트와 대소문자·공백 무시 비교
 * 3. QuizAttempt 이력 저장 (문항별)
 * 4. QuizEvent.totalAttempt 증가
 * 5. 정답 수 기준 보상 선물 결정 (min_correct 내림차순 → 첫 번째 매칭 규칙)
 */
@Service
@RequiredArgsConstructor
@Transactional
public class QuizService {

    private final EntityFinder entityFinder;
    private final QuizRepository quizRepository;
    private final QuizChoiceRepository quizChoiceRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizRewardRuleRepository quizRewardRuleRepository;

    public QuizResponse.Submit submitQuiz(String eventUrl, QuizRequest.Submit request) {
        BirthdayEvent event = entityFinder.getEventByUrlOrThrow(eventUrl);

        QuizEvent quizEvent = event.getQuizEvent();
        if (quizEvent == null) {
            throw new QuizException(ErrorCode.QUIZ_NOT_FOUND);
        }

        // 문항별 정답 검증 + 시도 기록
        int correctCount = 0;
        for (QuizRequest.Submit.AnswerItem answer : request.answers()) {
            Quiz quiz = quizRepository.findById(answer.quizId())
                    .orElseThrow(() -> new QuizException(ErrorCode.QUIZ_NOT_FOUND));

            boolean isCorrect = checkAnswer(quiz, answer.submittedAnswer());
            if (isCorrect) correctCount++;

            quizAttemptRepository.save(QuizAttempt.builder()
                    .quizEvent(quizEvent)
                    .quiz(quiz)
                    .submittedAnswer(answer.submittedAnswer())
                    .isCorrect(isCorrect)
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        // totalAttempt 증가 (dirty checking으로 자동 반영)
        quizEvent.setTotalAttempt(quizEvent.getTotalAttempt() + 1);

        // 보상 선물 결정: min_correct 내림차순 → 처음으로 correctCount >= minCorrect인 규칙
        // 예: successReward(minCorrect=3), failReward(minCorrect=0)
        //     correctCount=3 → successReward 매칭
        //     correctCount=1 → failReward 매칭
        int finalCorrect = correctCount;
        Gift rewardGift = quizRewardRuleRepository
                .findByQuizEventOrderByMinCorrectDesc(quizEvent).stream()
                .filter(r -> finalCorrect >= r.getMinCorrect())
                .findFirst()
                .map(QuizRewardRule::getGift)
                .orElse(null);

        return new QuizResponse.Submit(
                correctCount,
                request.answers().size(),
                rewardGift != null ? rewardGift.getGiftName() : null,
                rewardGift != null ? rewardGift.getGiftImageUrl() : null);
    }

    /**
     * 퀴즈 타입별 정답 검증
     * OBJECTIVE/OX: 선택한 텍스트가 is_correct=true인 선택지에 존재하는지
     * SUBJECTIVE: 저장된 정답 텍스트 목록과 대소문자·앞뒤공백 무시 비교
     */
    private boolean checkAnswer(Quiz quiz, String submittedAnswer) {
        List<QuizChoice> choices = quizChoiceRepository.findByQuiz(quiz);
        return switch (quiz.getQuizType()) {
            case OBJECTIVE, OX ->
                    choices.stream()
                            .filter(c -> c.getChoiceText().equals(submittedAnswer))
                            .anyMatch(QuizChoice::getIsCorrect);
            case SUBJECTIVE ->
                    choices.stream()
                            .anyMatch(c -> c.getChoiceText()
                                    .equalsIgnoreCase(submittedAnswer.trim()));
        };
    }
}
