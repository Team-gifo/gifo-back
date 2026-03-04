package com.gifo.backend.entity.capsule;

import com.gifo.backend.entity.event.BirthdayEvent;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "capsule_event")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CapsuleEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "capsule_event_id")
    private Long capsuleEventId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false, unique = true)
    private BirthdayEvent birthdayEvent;

    @Column(name = "max_draw_count")
    private Integer maxDrawCount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "capsuleEvent", fetch = FetchType.LAZY)
    private List<Capsule> capsules = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "capsuleEvent", fetch = FetchType.LAZY)
    private List<CapsuleDraw> capsuleDraws = new ArrayList<>();
}
