package com.gifo.backend.controller.quiz;

import com.gifo.backend.dto.quiz.QuizRequest;
import com.gifo.backend.dto.quiz.QuizResponse;
import com.gifo.backend.global.ApiResponse;
import com.gifo.backend.service.quiz.QuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/events")
@Tag(name = "Quiz API", description = "퀴즈 관련 API")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    /**
     * POST /events/{eventUrl}/quiz/submit - 퀴즈 전체 답안 제출
     * 채점 결과 + 정답 수 기준 보상 선물 반환
     */
    @PostMapping("/{eventUrl}/quiz/submit")
    @Operation(summary = "퀴즈 제출", description = "모든 퀴즈 답안을 일괄 제출합니다.")
    public ResponseEntity<ApiResponse<QuizResponse.Submit>> submit(
            @PathVariable String eventUrl,
            @RequestBody QuizRequest.Submit request) {

        QuizResponse.Submit response = quizService.submitQuiz(eventUrl, request);
        return ResponseEntity.ok(ApiResponse.success("퀴즈 제출 성공", response));
    }
}
