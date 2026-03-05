package com.gifo.backend.repository.direct;

import com.gifo.backend.entity.direct.DirectEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DirectEventRepository extends JpaRepository<DirectEvent, Long> {
}
