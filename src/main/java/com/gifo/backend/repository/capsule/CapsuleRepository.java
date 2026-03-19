package com.gifo.backend.repository.capsule;

import com.gifo.backend.entity.capsule.Capsule;
import com.gifo.backend.entity.capsule.CapsuleEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CapsuleRepository extends JpaRepository<Capsule, Long> {

    @Query("SELECT c FROM Capsule c LEFT JOIN FETCH c.gift WHERE c.capsuleEvent = :capsuleEvent")
    List<Capsule> findByCapsuleEvent(@Param("capsuleEvent") CapsuleEvent capsuleEvent);
}
