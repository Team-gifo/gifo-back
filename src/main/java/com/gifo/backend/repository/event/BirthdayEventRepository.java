package com.gifo.backend.repository.event;

import com.gifo.backend.entity.event.BirthdayEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BirthdayEventRepository extends JpaRepository<BirthdayEvent, Long> {

    boolean existsByEventUrl(String eventUrl);
}
