package com.gifo.backend.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gifo.backend.entity.capsule.Capsule;
import com.gifo.backend.entity.capsule.CapsuleEvent;
import com.gifo.backend.entity.event.BirthdayEvent;
import com.gifo.backend.entity.event.EventStatus;
import com.gifo.backend.entity.gift.Gift;
import com.gifo.backend.repository.capsule.CapsuleEventRepository;
import com.gifo.backend.repository.capsule.CapsuleRepository;
import com.gifo.backend.repository.event.BirthdayEventRepository;
import com.gifo.backend.repository.gift.GiftRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 캡슐 뽑기 전체 플로우 시나리오 테스트
 *
 * 캡슐 5개, maxDrawCount=5
 * 3번 뽑기 → 그 중 1개 select → 2번 더 뽑기 → 새로 뽑힌 것 중 하나로 select 변경
 * select 후에도 뽑기가 차단되지 않는 것을 검증
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CapsuleFullFlowTest {

    @Autowired MockMvc mockMvc;
    @Autowired EntityManager em;
    @Autowired BirthdayEventRepository birthdayEventRepository;
    @Autowired CapsuleEventRepository capsuleEventRepository;
    @Autowired CapsuleRepository capsuleRepository;
    @Autowired GiftRepository giftRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String eventUrl;

    @BeforeEach
    void setUp() {
        BirthdayEvent event = birthdayEventRepository.save(BirthdayEvent.builder()
                .eventUrl("CAP_FLOW").status(EventStatus.ACTIVE)
                .receiverName("김철수").senderName("박영희").title("생일 축하해")
                .expiredAt(LocalDateTime.now().plusDays(7)).build());
        eventUrl = event.getEventUrl();

        // 캡슐 이벤트: 최대 5회 뽑기
        CapsuleEvent capsuleEvent = capsuleEventRepository.save(CapsuleEvent.builder()
                .birthdayEvent(event).maxDrawCount(5).build());

        // 선물 5개 + 캡슐 5개
        String[] names = {"에어팟 프로", "양말 세트", "기프티콘", "텀블러", "쿠션"};
        String[] images = {"airpods.jpg", "socks.jpg", "gifticon.jpg", "tumbler.jpg", "cushion.jpg"};
        int[] weights = {30, 25, 20, 15, 10};

        for (int i = 0; i < 5; i++) {
            Gift gift = giftRepository.save(Gift.builder()
                    .giftName(names[i]).giftImageUrl("https://img/" + images[i])
                    .description(names[i] + " 선물!").isProbabilityPublic(true).build());
            capsuleRepository.save(Capsule.builder()
                    .capsuleEvent(capsuleEvent).gift(gift).weight(weights[i]).build());
        }

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("전체 시나리오: 3번 뽑기 → select → 2번 더 뽑기 → 새 캡슐로 select 변경")
    void fullFlow_drawSelectDrawReselect() throws Exception {
        // ── 초기 상태 확인 ──
        mockMvc.perform(get("/events/{eventUrl}", eventUrl))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.gacha.playCount").value(5))
                .andExpect(jsonPath("$.data.content.gacha.remainingDrawCount").value(5))
                .andExpect(jsonPath("$.data.content.gacha.selected").value(false))
                .andExpect(jsonPath("$.data.content.gacha.drawHistory.length()").value(0));

        // ── 1~3번째 뽑기 ──
        Long[] drawnCapsuleIds = new Long[5];

        for (int i = 0; i < 3; i++) {
            String result = mockMvc.perform(post("/events/{eventUrl}/capsules/draw", eventUrl))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.capsuleId").isNumber())
                    .andExpect(jsonPath("$.data.drawnAt").isString()) // 당첨 시각
                    .andReturn().getResponse().getContentAsString();
            drawnCapsuleIds[i] = objectMapper.readTree(result).path("data").path("capsuleId").asLong();
        }

        // 3번 뽑기 후 상태 확인: remainingDrawCount=2, drawHistory=3건
        mockMvc.perform(get("/events/{eventUrl}", eventUrl))
                .andExpect(jsonPath("$.data.content.gacha.remainingDrawCount").value(2))
                .andExpect(jsonPath("$.data.content.gacha.drawHistory.length()").value(3))
                .andExpect(jsonPath("$.data.content.gacha.selected").value(false));

        // ── 첫 번째 select: 1번째로 뽑은 캡슐 선택 ──
        mockMvc.perform(post("/events/{eventUrl}/capsules/select", eventUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"capsuleId\":" + drawnCapsuleIds[0] + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.giftName").isString());

        // select 후 상태: selected=true, remainingDrawCount=2 (select가 뽑기를 차단하지 않음)
        mockMvc.perform(get("/events/{eventUrl}", eventUrl))
                .andExpect(jsonPath("$.data.content.gacha.selected").value(true))
                .andExpect(jsonPath("$.data.content.gacha.remainingDrawCount").value(2));

        // ── select 이후에도 4~5번째 뽑기 가능 ──
        for (int i = 3; i < 5; i++) {
            String result = mockMvc.perform(post("/events/{eventUrl}/capsules/draw", eventUrl))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.capsuleId").isNumber())
                    .andExpect(jsonPath("$.data.drawnAt").isString()) // 당첨 시각
                    .andReturn().getResponse().getContentAsString();
            drawnCapsuleIds[i] = objectMapper.readTree(result).path("data").path("capsuleId").asLong();
        }

        // 5번 모두 뽑은 후 상태: remainingDrawCount=0, drawHistory=5건
        mockMvc.perform(get("/events/{eventUrl}", eventUrl))
                .andExpect(jsonPath("$.data.content.gacha.remainingDrawCount").value(0))
                .andExpect(jsonPath("$.data.content.gacha.drawHistory.length()").value(5))
                .andExpect(jsonPath("$.data.content.gacha.selected").value(true));

        // 6번째 뽑기 시도 → 횟수 초과 에러
        mockMvc.perform(post("/events/{eventUrl}/capsules/draw", eventUrl))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CAPSULE_DRAW_LIMIT_EXCEEDED"));

        // ── select 변경: 새로 뽑은 4번째 캡슐로 변경 ──
        mockMvc.perform(post("/events/{eventUrl}/capsules/select", eventUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"capsuleId\":" + drawnCapsuleIds[3] + "}"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.giftName").isString());

        // 최종 상태 확인: selected=true, drawHistory에서 selected=true는 정확히 1개
        String finalState = mockMvc.perform(get("/events/{eventUrl}", eventUrl))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.gacha.selected").value(true))
                .andExpect(jsonPath("$.data.content.gacha.drawHistory.length()").value(5))
                .andReturn().getResponse().getContentAsString();

        JsonNode drawHistory = objectMapper.readTree(finalState)
                .path("data").path("content").path("gacha").path("drawHistory");
        long selectedCount = 0;
        Long selectedCapsuleId = null;
        for (JsonNode draw : drawHistory) {
            if (draw.path("selected").asBoolean()) {
                selectedCount++;
                selectedCapsuleId = draw.path("capsuleId").asLong();
            }
        }
        Assertions.assertEquals(1, selectedCount, "selected=true인 캡슐은 정확히 1개여야 합니다");
        Assertions.assertEquals(drawnCapsuleIds[3], selectedCapsuleId,
                "4번째로 뽑은 캡슐이 최종 선택되어야 합니다");
    }
}
