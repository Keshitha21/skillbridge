package com.skillbridge.backend.repository;

import com.skillbridge.backend.entity.PortfolioItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PortfolioItemRepository extends JpaRepository<PortfolioItem, Long> {

    List<PortfolioItem> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<PortfolioItem> findByUserIdAndVisibility(Long userId, PortfolioItem.Visibility visibility);
}
