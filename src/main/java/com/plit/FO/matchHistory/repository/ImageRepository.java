package com.plit.FO.matchHistory.repository;

import com.plit.FO.matchHistory.entity.ImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ImageRepository extends JpaRepository<ImageEntity, Long> {

    // 이름 + 타입 조합으로 검색
    Optional<ImageEntity> findByNameAndType(String name, String type);

    // 이미 존재하는지 확인
    boolean existsByNameAndType(String name, String type);

    // 특정 타입의 이미지 전부 가져옴
    List<ImageEntity> findByType(String type);
}