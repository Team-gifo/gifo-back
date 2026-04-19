package com.gifo.backend.curate.prompt;

import com.gifo.backend.curate.dto.SurveyRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 큐레이션용 프롬프트를 리소스에서 로드하고 변수를 치환합니다.
 * 새 게임 추가 시: prompts/games/{게임키}.txt 파일만 추가하고 GAME_KEYS에 키를 등록하면 됩니다.
 */
@Slf4j
@Component
public class CuratePromptLoader {

    private static final String BASE_PROMPT_PATH = "prompts/curate-base.txt";
    private static final String GAMES_DIR = "prompts/games/";
    private static final String BGM_LIST_PATH = "prompts/bgm-list.txt";
    private static final String GAME_SPECS_PLACEHOLDER = "{{game_specs}}";
    private static final String BGM_LIST_PLACEHOLDER = "{{bgm_list}}";

    private static final List<String> GAME_KEYS = List.of("gacha", "quiz", "unboxing");

    public String loadPrompt(SurveyRequestDto survey) {
        Map<String, String> variables = Map.of(
                "relationship", nullToEmpty(survey.getRelationship()),
                "situation", nullToEmpty(survey.getSituation()),
                "tone", nullToEmpty(survey.getTone()),
                "targetAge", nullToEmpty(survey.getTargetAge()),
                "targetName", survey.getTargetName() != null && !survey.getTargetName().isBlank()
                        ? survey.getTargetName()
                        : "(없음)"
        );
        return loadPrompt(variables);
    }

    public String loadPrompt(Map<String, String> variables) {
        String base = loadResource(BASE_PROMPT_PATH);
        if (base == null) {
            throw new IllegalStateException("Base prompt not found: " + BASE_PROMPT_PATH);
        }

        String gameSpecs = loadGameSpecs();
        String bgmList = loadBgmList();
        String prompt = base
                .replace(GAME_SPECS_PLACEHOLDER, gameSpecs)
                .replace(BGM_LIST_PLACEHOLDER, bgmList);

        for (Map.Entry<String, String> e : variables.entrySet()) {
            prompt = prompt.replace("{{" + e.getKey() + "}}", e.getValue());
        }

        return prompt.trim();
    }

    private String loadBgmList() {
        String content = loadResource(BGM_LIST_PATH);
        return content != null ? content.trim() : "track_sweet_01 (기본)";
    }

    /** 등록된 게임 스펙 파일들을 순서대로 읽어 하나의 문자열로 합칩니다. */
    private String loadGameSpecs() {
        return GAME_KEYS.stream()
                .map(key -> {
                    String path = GAMES_DIR + key + ".txt";
                    String content = loadResource(path);
                    return content != null ? content.trim() : "";
                })
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining("\n\n"));
    }

    private String loadResource(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            try (InputStream is = resource.getInputStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.warn("Prompt resource not found or unreadable: {}", path);
            return null;
        }
    }

    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }
}
