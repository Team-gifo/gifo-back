package com.gifo.backend.repository.capsule;

import com.gifo.backend.entity.capsule.CapsuleDraw;
import com.gifo.backend.entity.capsule.CapsuleEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CapsuleDrawRepository extends JpaRepository<CapsuleDraw, Long> {

    long countByCapsuleEvent(CapsuleEvent capsuleEvent);

    void deleteByCapsuleEvent(CapsuleEvent capsuleEvent);
}
