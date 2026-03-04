package com.gifo.backend.entity.event;

import com.gifo.backend.entity.capsule.CapsuleEvent;
import com.gifo.backend.entity.direct.DirectEvent;
import com.gifo.backend.entity.quiz.QuizEvent;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "birthday_event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BirthdayEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "event_url", unique = true)
    private String eventUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private EventStatus status;

    @Column(name = "password")
    private String password;

    @Column(name = "receiver_name")
    private String receiverName;

    @Column(name = "sender_name")
    private String senderName;

    @Column(name = "title")
    private String title;

    @Column(name = "bgm_url")
    private String bgmUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Builder.Default
    @OneToMany(mappedBy = "birthdayEvent", fetch = FetchType.LAZY)
    private List<Memory> memories = new ArrayList<>();

    @OneToOne(mappedBy = "birthdayEvent", fetch = FetchType.LAZY)
    private CapsuleEvent capsuleEvent;

    @OneToOne(mappedBy = "birthdayEvent", fetch = FetchType.LAZY)
    private DirectEvent directEvent;

    @OneToOne(mappedBy = "birthdayEvent", fetch = FetchType.LAZY)
    private QuizEvent quizEvent;
}
