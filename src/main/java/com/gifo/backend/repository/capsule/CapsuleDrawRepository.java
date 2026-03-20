package com.gifo.backend.repository.capsule;

import com.gifo.backend.entity.capsule.CapsuleDraw;
import com.gifo.backend.entity.capsule.CapsuleEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface CapsuleDrawRepository extends JpaRepository<CapsuleDraw, Long> {

    long countByCapsuleEvent(CapsuleEvent capsuleEvent);

    @Query("SELECT DISTINCT d.capsule.capsuleId FROM CapsuleDraw d WHERE d.capsuleEvent = :capsuleEvent")
    Set<Long> findDrawnCapsuleIdsByCapsuleEvent(@Param("capsuleEvent") CapsuleEvent capsuleEvent);

    @Query("SELECT d FROM CapsuleDraw d JOIN FETCH d.capsule c JOIN FETCH c.gift WHERE d.capsuleEvent = :capsuleEvent ORDER BY d.drawId ASC")
    List<CapsuleDraw> findByCapsuleEventWithGift(@Param("capsuleEvent") CapsuleEvent capsuleEvent);

    Optional<CapsuleDraw> findByCapsuleEventAndCapsule_CapsuleId(CapsuleEvent capsuleEvent, Long capsuleId);

    boolean existsByCapsuleEventAndSelectedTrue(CapsuleEvent capsuleEvent);

    void deleteByCapsuleEvent(CapsuleEvent capsuleEvent);
}
