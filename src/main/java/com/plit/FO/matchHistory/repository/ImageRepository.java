package com.plit.FO.matchHistory.repository;

import com.plit.FO.matchHistory.entity.ImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ImageRepository extends JpaRepository<ImageEntity, Long> {
    Optional<ImageEntity> findByNameAndType(String name, String type);
    boolean existsByNameAndType(String name, String type);

    List<ImageEntity> findByType(String type);
}