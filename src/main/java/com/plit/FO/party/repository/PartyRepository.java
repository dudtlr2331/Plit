package com.plit.FO.party.repository;

import com.plit.FO.party.entity.PartyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartyRepository extends JpaRepository<PartyEntity, Long> {
//    findAll() – 전체 조회
//    save(entity) – 저장 및 수정
//    findById(id) – id로 조회
//    deleteById(id) – 삭제
    List<PartyEntity> findByPartyType(String partyType);
}
