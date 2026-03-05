package com.gifo.backend.repository.capsule;

import com.gifo.backend.entity.capsule.CapsuleEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CapsuleEventRepository extends JpaRepository<CapsuleEvent, Long> {
}
