package com.gifo.backend.dto.quiz;

import java.util.List;

/**
 * 퀴즈 도메인 요청 DTO
 */
public class QuizRequest {

    /**
     * POST /events/{eventUrl}/quiz/submit 요청
     * 모든 퀴즈 답안을 한번에 일괄 제출
     */
    public record Submit(List<AnswerItem> answers) {

        /**
         * 퀴즈 문항별 답안
         * quizId: 퀴즈 PK
         * submittedAnswer: 사용자가 제출한 답
         */
        public record AnswerItem(
                Long quizId,
                String submittedAnswer
        ) {}
    }
}
