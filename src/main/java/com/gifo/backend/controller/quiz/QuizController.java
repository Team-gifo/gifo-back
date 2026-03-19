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
     * POST /events/{eventUrl}/quiz/result - 퀴즈 최종 결과 저장
     * 프론트에서 채점 완료 후 정답 수만 전송하여 저장
     */
    @PostMapping("/{eventUrl}/quiz/result")
    @Operation(summary = "퀴즈 결과 저장", description = "프론트에서 채점한 최종 정답 수를 저장합니다.")
    public ResponseEntity<ApiResponse<QuizResponse.Result>> saveResult(
            @PathVariable String eventUrl,
            @RequestBody QuizRequest.Result request) {

        QuizResponse.Result response = quizService.saveResult(eventUrl, request);
        return ResponseEntity.ok(ApiResponse.success("퀴즈 결과 저장 성공", response));
    }
}
