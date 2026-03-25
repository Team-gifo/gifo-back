package com.gifo.backend.integration;

import com.gifo.backend.entity.direct.DirectEvent;
import com.gifo.backend.entity.event.BirthdayEvent;
import com.gifo.backend.entity.event.EventStatus;
import com.gifo.backend.entity.gift.Gift;
import com.gifo.backend.repository.direct.DirectEventRepository;
import com.gifo.backend.repository.event.BirthdayEventRepository;
import com.gifo.backend.repository.gift.GiftRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UnboxingApiTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    EntityManager em;
    @Autowired
    BirthdayEventRepository birthdayEventRepository;
    @Autowired
    DirectEventRepository directEventRepository;
    @Autowired
    GiftRepository giftRepository;

    private String eventUrl;

    @BeforeEach
    void setUp() {
        BirthdayEvent event = birthdayEventRepository.save(BirthdayEvent.builder()
                .eventUrl("UNBOX123")
                .status(EventStatus.ACTIVE)
                .receiverName("김철수")
                .senderName("박영희")
                .title("생일 축하해")
                .expiredAt(LocalDateTime.now().plusDays(7))
                .build());
        eventUrl = event.getEventUrl();

        Gift gift = giftRepository.save(Gift.builder()
                .giftName("에어팟 프로")
                .giftImageUrl("https://img/airpods.jpg")
                .description("축하해요!")
                .isProbabilityPublic(true)
                .build());

        directEventRepository.save(DirectEvent.builder()
                .birthdayEvent(event)
                .gift(gift)
                .beforeImageUrl("https://img/box.jpg")
                .beforeDescription("생일 축하해! 이 상자를 열어봐!")
                .build());

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("1. GET /events/{eventUrl} - 언박싱 이벤트 조회")
    void getEvent_unboxing() throws Exception {
        mockMvc.perform(get("/events/{eventUrl}", eventUrl))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.user").value("김철수"))
                .andExpect(jsonPath("$.data.content.gacha").isEmpty())
                .andExpect(jsonPath("$.data.content.quiz").isEmpty())
                .andExpect(jsonPath("$.data.content.unboxing").isNotEmpty())
                .andExpect(jsonPath("$.data.content.unboxing.beforeOpen.imageUrl").value("https://img/box.jpg"))
                .andExpect(jsonPath("$.data.content.unboxing.beforeOpen.description").value("생일 축하해! 이 상자를 열어봐!"))
                .andExpect(jsonPath("$.data.content.unboxing.afterOpen.itemName").value("에어팟 프로"))
                .andExpect(jsonPath("$.data.content.unboxing.afterOpen.imageUrl").value("https://img/airpods.jpg"));
    }

    @Test
    @DisplayName("2. DELETE /progress - 언박싱은 초기화 대상 없음 (정상 응답)")
    void resetProgress_unboxing() throws Exception {
        mockMvc.perform(delete("/events/{eventUrl}/progress", eventUrl))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        // 리셋 후에도 동일하게 조회됨
        mockMvc.perform(get("/events/{eventUrl}", eventUrl))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.unboxing.afterOpen.itemName").value("에어팟 프로"));
    }
}
