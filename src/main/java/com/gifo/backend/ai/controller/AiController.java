package com.gifo.backend.ai.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/azure")
@Tag(name = "Azure OpenAI", description = "Azure OpenAI 연결 테스트용 API")
@RequiredArgsConstructor
public class AiController {

    private final ChatClient chatClient;

    /**
     * Azure OpenAI 채팅 API
     * Parameter: userMessage
     * Return: String
     * Description: 메시지 텍스트를 그대로 보내면 Azure OpenAI 응답.
     * Example:
     * Request:
     * {
     * "userMessage": "Gifo 의미 알려줘"
     * }
     */
    @PostMapping(value = "/chat", consumes = "text/plain")
    @Operation(summary = "채팅", description = "메시지 텍스트를 그대로 보내면 Azure OpenAI 응답.")
    public String chat(@RequestBody String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return "메시지를 입력해주세요.";
        }

        String response = chatClient.prompt()
                .user(userMessage)
                .call()
                .content();

        return response != null ? response : "Azure OpenAI 응답이 비어 있습니다.";
    }
}

