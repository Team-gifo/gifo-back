package com.gifo.backend.repository.capsule;

import com.gifo.backend.entity.capsule.Capsule;
import com.gifo.backend.entity.capsule.CapsuleEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CapsuleRepository extends JpaRepository<Capsule, Long> {

    List<Capsule> findByCapsuleEvent(CapsuleEvent capsuleEvent);
}
