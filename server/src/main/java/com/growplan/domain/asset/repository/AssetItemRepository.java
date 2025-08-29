package com.growplan.domain.asset.repository;

import com.growplan.domain.asset.entity.AssetItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetItemRepository extends JpaRepository<AssetItem, Long> {
}
