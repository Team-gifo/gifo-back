package com.gifo.backend.controller.quiz;

import com.gifo.backend.dto.quiz.QuizRequest;
import com.gifo.backend.dto.quiz.QuizResponse;
import com.gifo.backend.global.ApiResponse;
import com.gifo.backend.service.quiz.QuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
     * POST /events/{eventUrl}/quiz/answer - 문제별 결과 저장
     * 정답을 맞추거나 playLimit 소진 시 호출
     */
    @PostMapping("/{eventUrl}/quiz/answer")
    @Operation(summary = "퀴즈 문제별 결과 저장", description = "문제 1개의 풀이 결과(정답/오답)를 저장합니다.")
    public ResponseEntity<ApiResponse<QuizResponse.Answer>> saveAnswer(
            @PathVariable String eventUrl,
            @Valid @RequestBody QuizRequest.Answer request) {

        QuizResponse.Answer response = quizService.saveAnswerWithAttempts(eventUrl, request);
        return ResponseEntity.ok(ApiResponse.success("문제 결과 저장 성공", response));
    }

    /**
     * POST /events/{eventUrl}/quiz/result - 퀴즈 최종 결과 저장
     * 모든 문제 완료 후 서버가 DB에서 정답 수를 계산하여 보상 판정
     */
    @PostMapping("/{eventUrl}/quiz/result")
    @Operation(summary = "퀴즈 결과 저장", description = "모든 문제 완료 후 최종 보상을 판정합니다.")
    public ResponseEntity<ApiResponse<QuizResponse.Result>> saveResult(
            @PathVariable String eventUrl,
            @Valid @RequestBody QuizRequest.Result request) {

        QuizResponse.Result response = quizService.saveResult(eventUrl, request);
        return ResponseEntity.ok(ApiResponse.success("퀴즈 결과 저장 성공", response));
    }
}
