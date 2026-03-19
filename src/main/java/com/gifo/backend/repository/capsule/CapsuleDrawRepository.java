package com.gifo.backend.repository.capsule;

import com.gifo.backend.entity.capsule.Capsule;
import com.gifo.backend.entity.capsule.CapsuleDraw;
import com.gifo.backend.entity.capsule.CapsuleEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CapsuleDrawRepository extends JpaRepository<CapsuleDraw, Long> {

    long countByCapsuleEvent(CapsuleEvent capsuleEvent);

    @Query("SELECT DISTINCT d.capsule FROM CapsuleDraw d WHERE d.capsuleEvent = :capsuleEvent")
    List<Capsule> findDrawnCapsulesByCapsuleEvent(@Param("capsuleEvent") CapsuleEvent capsuleEvent);

    void deleteByCapsuleEvent(CapsuleEvent capsuleEvent);
}
