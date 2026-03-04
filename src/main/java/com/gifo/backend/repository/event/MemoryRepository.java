package com.gifo.backend.repository.event;

import com.gifo.backend.entity.event.Memory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemoryRepository extends JpaRepository<Memory, Long> {
}
