package com.gifo.backend.entity.capsule;

import com.gifo.backend.entity.gift.Gift;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "capsule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Capsule {

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

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "capsule", fetch = FetchType.LAZY)
    private List<CapsuleDraw> capsuleDraws = new ArrayList<>();
}
