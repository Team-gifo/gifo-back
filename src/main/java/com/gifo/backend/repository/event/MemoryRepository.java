package com.gifo.backend.repository.event;

import com.gifo.backend.entity.event.BirthdayEvent;
import com.gifo.backend.entity.event.Memory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemoryRepository extends JpaRepository<Memory, Long> {

    List<Memory> findByBirthdayEventOrderBySortOrderAsc(BirthdayEvent birthdayEvent);
}
