package com.gifo.backend.repository.event;

import com.gifo.backend.entity.event.BirthdayEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BirthdayEventRepository extends JpaRepository<BirthdayEvent, Long> {

    boolean existsByEventUrl(String eventUrl);

    Optional<BirthdayEvent> findByEventUrl(String eventUrl);
}
