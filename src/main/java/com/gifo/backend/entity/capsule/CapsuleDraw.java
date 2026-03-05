package com.gifo.backend.entity.capsule;

import com.gifo.backend.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "capsule_draw")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class CapsuleDraw extends BaseEntity {

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
}
