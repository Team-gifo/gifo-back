package com.gifo.backend.repository.gift;

import com.gifo.backend.entity.gift.Gift;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GiftRepository extends JpaRepository<Gift, Long> {
}
