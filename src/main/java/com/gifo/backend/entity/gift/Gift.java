package com.gifo.backend.entity.gift;

import com.gifo.backend.entity.capsule.Capsule;
import com.gifo.backend.entity.direct.DirectEvent;
import com.gifo.backend.entity.quiz.QuizRewardRule;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "gift")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Gift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gift_id")
    private Long giftId;

    @Column(name = "gift_name")
    private String giftName;

    @Column(name = "gift_image_url")
    private String giftImageUrl;

    @Column(name = "description")
    private String description;

    @Column(name = "is_hidden")
    private Boolean isHidden;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "gift", fetch = FetchType.LAZY)
    private List<Capsule> capsules = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "gift", fetch = FetchType.LAZY)
    private List<DirectEvent> directEvents = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "gift", fetch = FetchType.LAZY)
    private List<QuizRewardRule> quizRewardRules = new ArrayList<>();
}
