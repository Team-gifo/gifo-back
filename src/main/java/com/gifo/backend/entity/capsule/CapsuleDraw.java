package com.gifo.backend.entity.capsule;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "capsule_draw")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CapsuleDraw {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "draw_id")
    private Long drawId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "capsule_event_id", nullable = false)
    private CapsuleEvent capsuleEvent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "capsule_id", nullable = false)
    private Capsule capsule;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
