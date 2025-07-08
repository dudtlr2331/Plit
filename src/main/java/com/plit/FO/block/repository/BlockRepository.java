package com.plit.FO.block.repository;

import com.plit.FO.block.entity.BlockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlockRepository extends JpaRepository<BlockEntity, Integer> {

    Optional<BlockEntity> findById(Integer blockerId);
    List<BlockEntity> findAllByBlockerIdAndIsReleasedFalse(Integer blockerId);
    boolean existsByBlockerIdAndBlockedUserIdAndIsReleasedFalse(Integer blockerId, Integer blockedUserId);
}
