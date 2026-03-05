package com.gifo.backend.entity.capsule;

import com.gifo.backend.entity.BaseEntity;
import com.gifo.backend.entity.gift.Gift;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "capsule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class Capsule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "capsule_id")
    private Long capsuleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "capsule_event_id", nullable = false)
    private CapsuleEvent capsuleEvent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gift_id", nullable = false)
    private Gift gift;

    @Column(name = "weight")
    private Integer weight;

    @Builder.Default
    @OneToMany(mappedBy = "capsule", fetch = FetchType.LAZY)
    private List<CapsuleDraw> capsuleDraws = new ArrayList<>();
}
