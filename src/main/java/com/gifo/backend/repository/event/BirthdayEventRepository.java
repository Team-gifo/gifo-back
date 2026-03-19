package com.gifo.backend.repository.event;

import com.gifo.backend.entity.event.BirthdayEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BirthdayEventRepository extends JpaRepository<BirthdayEvent, Long> {

    boolean existsByEventUrl(String eventUrl);

    Optional<BirthdayEvent> findByEventUrl(String eventUrl);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM BirthdayEvent e WHERE e.eventUrl = :eventUrl")
    Optional<BirthdayEvent> findByEventUrlForUpdate(@Param("eventUrl") String eventUrl);
}
