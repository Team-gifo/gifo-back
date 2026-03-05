package com.gifo.backend.entity.direct;

import com.gifo.backend.entity.BaseEntity;
import com.gifo.backend.entity.event.BirthdayEvent;
import com.gifo.backend.entity.gift.Gift;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "direct_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
public class DirectEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "direct_event_id")
    private Long directEventId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false, unique = true)
    private BirthdayEvent birthdayEvent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gift_id", nullable = false)
    private Gift gift;

    @Column(name = "before_image_url")
    private String beforeImageUrl;

    @Column(name = "before_description")
    private String beforeDescription;
}
