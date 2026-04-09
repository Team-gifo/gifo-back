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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CapsuleApiTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    EntityManager em;
    @Autowired
    BirthdayEventRepository birthdayEventRepository;
    @Autowired
    CapsuleEventRepository capsuleEventRepository;
    @Autowired
    CapsuleRepository capsuleRepository;
    @Autowired
    GiftRepository giftRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String eventUrl;

    @BeforeEach
    void setUp() {
        // 이벤트 생성
        BirthdayEvent event = birthdayEventRepository.save(BirthdayEvent.builder()
                .eventUrl("TEST1234")
                .status(EventStatus.ACTIVE)
                .receiverName("김철수")
                .senderName("박영희")
                .title("생일 축하해")
                .expiredAt(LocalDateTime.now().plusDays(7))
                .build());
        eventUrl = event.getEventUrl();

        // 캡슐 이벤트 (최대 3회 뽑기)
        CapsuleEvent capsuleEvent = capsuleEventRepository.save(CapsuleEvent.builder()
                .birthdayEvent(event)
                .maxDrawCount(3)
                .build());

        // 선물 3개 + 캡슐 3개 (가중치: 50, 30, 20)
        Gift gift1 = giftRepository.save(Gift.builder()
                .giftName("에어팟 프로").giftImageUrl("https://img/airpods.jpg")
                .description("축하해요!").isProbabilityPublic(true).build());
        Gift gift2 = giftRepository.save(Gift.builder()
                .giftName("양말 세트").giftImageUrl("https://img/socks.jpg")
                .description("따뜻한 양말!").isProbabilityPublic(true).build());
        Gift gift3 = giftRepository.save(Gift.builder()
                .giftName("기프티콘").giftImageUrl("https://img/gifticon.jpg")
                .description("커피 한잔!").isProbabilityPublic(false).build());

        capsuleRepository.save(Capsule.builder()
                .capsuleEvent(capsuleEvent).gift(gift1).weight(50).build());
        capsuleRepository.save(Capsule.builder()
                .capsuleEvent(capsuleEvent).gift(gift2).weight(30).build());
        capsuleRepository.save(Capsule.builder()
                .capsuleEvent(capsuleEvent).gift(gift3).weight(20).build());

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("1. GET /events/{eventUrl} - 캡슐 이벤트 조회 (초기 상태)")
    void getEvent_capsule_initial() throws Exception {
        mockMvc.perform(get("/events/{eventUrl}", eventUrl))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.user").value("김철수"))
                .andExpect(jsonPath("$.data.content.gacha.playCount").value(3))
                .andExpect(jsonPath("$.data.content.gacha.remainingDrawCount").value(3))
                .andExpect(jsonPath("$.data.content.gacha.selected").value(false))
                .andExpect(jsonPath("$.data.content.gacha.list.length()").value(3))
                .andExpect(jsonPath("$.data.content.gacha.drawHistory.length()").value(0))
                .andExpect(jsonPath("$.data.content.quiz").isEmpty())
                .andExpect(jsonPath("$.data.content.unboxing").isEmpty());
    }

    @Test
    @DisplayName("2. POST /capsules/draw - 캡슐 1개 뽑기")
    void drawCapsule() throws Exception {
        mockMvc.perform(post("/events/{eventUrl}/capsules/draw", eventUrl))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.capsuleId").isNumber())
                .andExpect(jsonPath("$.data.giftName").isString())
                .andExpect(jsonPath("$.data.giftImageUrl").isString())
                .andExpect(jsonPath("$.data.description").isString());
    }

    @Test
    @DisplayName("3. 3번 뽑기 후 조회 - remainingDrawCount=0, drawHistory=3건")
    void drawThreeTimes_thenGetEvent() throws Exception {
        // 3번 뽑기
        mockMvc.perform(post("/events/{eventUrl}/capsules/draw", eventUrl));
        mockMvc.perform(post("/events/{eventUrl}/capsules/draw", eventUrl));
        mockMvc.perform(post("/events/{eventUrl}/capsules/draw", eventUrl));

        // 조회 - 히스토리 확인
        mockMvc.perform(get("/events/{eventUrl}", eventUrl))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.gacha.remainingDrawCount").value(0))
                .andExpect(jsonPath("$.data.content.gacha.drawHistory.length()").value(3))
                .andExpect(jsonPath("$.data.content.gacha.drawHistory[0].capsuleId").isNumber())
                .andExpect(jsonPath("$.data.content.gacha.drawHistory[0].giftName").isString())
                .andExpect(jsonPath("$.data.content.gacha.drawHistory[0].selected").value(false))
                .andExpect(jsonPath("$.data.content.gacha.drawHistory[0].drawnAt").isString());
    }

    @Test
    @DisplayName("4. 뽑기 횟수 초과 시 에러")
    void drawExceedLimit() throws Exception {
        mockMvc.perform(post("/events/{eventUrl}/capsules/draw", eventUrl));
        mockMvc.perform(post("/events/{eventUrl}/capsules/draw", eventUrl));
        mockMvc.perform(post("/events/{eventUrl}/capsules/draw", eventUrl));

        mockMvc.perform(post("/events/{eventUrl}/capsules/draw", eventUrl))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CAPSULE_DRAW_LIMIT_EXCEEDED"));
    }

    @Test
    @DisplayName("5. POST /capsules/select - 뽑힌 캡슐 선택")
    void selectCapsule() throws Exception {
        // 1번 뽑기
        String drawResult = mockMvc.perform(post("/events/{eventUrl}/capsules/draw", eventUrl))
                .andReturn().getResponse().getContentAsString();

        // capsuleId 추출 (ObjectMapper 사용)
        JsonNode drawJson = objectMapper.readTree(drawResult);
        Long capsuleId = drawJson.path("data").path("capsuleId").asLong();

        // 선택
        mockMvc.perform(post("/events/{eventUrl}/capsules/select", eventUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"capsuleId\":" + capsuleId + "}"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.giftName").isString())
                .andExpect(jsonPath("$.data.giftImageUrl").isString())
                .andExpect(jsonPath("$.data.description").isString());

        // 조회 - selected=true 확인
        mockMvc.perform(get("/events/{eventUrl}", eventUrl))
                .andExpect(jsonPath("$.data.content.gacha.selected").value(true))
                .andExpect(jsonPath("$.data.content.gacha.drawHistory[0].selected").value(true));
    }

    @Test
    @DisplayName("5-1. POST /capsules/select - 선택 변경 (재선택)")
    void reselectCapsule() throws Exception {
        // 2번 뽑기
        String draw1 = mockMvc.perform(post("/events/{eventUrl}/capsules/draw", eventUrl))
                .andReturn().getResponse().getContentAsString();
        String draw2 = mockMvc.perform(post("/events/{eventUrl}/capsules/draw", eventUrl))
                .andReturn().getResponse().getContentAsString();

        Long capsuleId1 = objectMapper.readTree(draw1).path("data").path("capsuleId").asLong();
        Long capsuleId2 = objectMapper.readTree(draw2).path("data").path("capsuleId").asLong();

        // 첫 번째 선택
        mockMvc.perform(post("/events/{eventUrl}/capsules/select", eventUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"capsuleId\":" + capsuleId1 + "}"))
                .andExpect(status().isOk());

        // 두 번째 선택 (변경) — 에러 없이 성공해야 함
        mockMvc.perform(post("/events/{eventUrl}/capsules/select", eventUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"capsuleId\":" + capsuleId2 + "}"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        // 조회 - 두 번째 캡슐만 selected=true, 첫 번째는 해제
        String getResult = mockMvc.perform(get("/events/{eventUrl}", eventUrl))
                .andDo(print())
                .andExpect(jsonPath("$.data.content.gacha.selected").value(true))
                .andReturn().getResponse().getContentAsString();

        // drawHistory에서 selected=true인 항목이 정확히 1개인지 검증
        JsonNode drawHistory = objectMapper.readTree(getResult)
                .path("data").path("content").path("gacha").path("drawHistory");
        long selectedCount = 0;
        for (JsonNode draw : drawHistory) {
            if (draw.path("selected").asBoolean()) selectedCount++;
        }
        Assertions.assertEquals(1, selectedCount, "selected=true인 캡슐은 정확히 1개여야 합니다");
    }

    @Test
    @DisplayName("6. DELETE /progress - 리셋 후 히스토리 초기화")
    void resetProgress() throws Exception {
        // 2번 뽑기
        mockMvc.perform(post("/events/{eventUrl}/capsules/draw", eventUrl));
        mockMvc.perform(post("/events/{eventUrl}/capsules/draw", eventUrl));

        // 리셋
        mockMvc.perform(delete("/events/{eventUrl}/progress", eventUrl))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        // 조회 - 초기 상태 복원
        mockMvc.perform(get("/events/{eventUrl}", eventUrl))
                .andExpect(jsonPath("$.data.content.gacha.remainingDrawCount").value(3))
                .andExpect(jsonPath("$.data.content.gacha.drawHistory.length()").value(0));
    }
}
