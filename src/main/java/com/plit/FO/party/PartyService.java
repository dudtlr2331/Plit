package com.plit.FO.party;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PartyService {

    private final PartyRepository partyRepository;
    private final PartyFindPositionRepository positionRepository;

    public PartyService(PartyRepository partyRepository, PartyFindPositionRepository positionRepository) {
        this.partyRepository = partyRepository;
        this.positionRepository = positionRepository;
    }

    /** 타입별 파티 조회 (REST API용) */
    public List<PartyDTO> findByPartyType(String partyType) {
        return partyRepository.findByPartyType(partyType).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /** 파티 단건 조회 (수정 폼 등에 사용) */
    public PartyDTO getParty(Long id) {
        PartyEntity party = partyRepository.findById(id).orElseThrow();
        return toDTO(party);
    }

    /** 파티 등록 */
    @Transactional
    public void saveParty(PartyDTO dto) {
        PartyEntity party = toEntity(dto);
        party.setPartyCreateDate(LocalDateTime.now());
        PartyEntity saved = partyRepository.save(party);
        savePositions(saved, dto.getPositions());
    }

    /** 파티 수정 */
    @Transactional
    public void updateParty(Long id, PartyDTO dto) {
        PartyEntity party = partyRepository.findById(id).orElseThrow();

        party.setPartyName(dto.getPartyName());
        party.setPartyType(dto.getPartyType());
        party.setPartyEndTime(dto.getPartyEndTime());
        party.setPartyStatus(dto.getPartyStatus());
        party.setPartyHeadcount(dto.getPartyHeadcount());
        party.setPartyMax(dto.getPartyMax());
        party.setMemo(dto.getMemo());
        party.setMainPosition(dto.getMainPosition());

        // 연관된 모집 포지션 재등록
        positionRepository.deleteByParty(party);
        savePositions(party, dto.getPositions());
    }

    /** 파티 삭제 */
    @Transactional
    public void deleteParty(Long id) {
        PartyEntity party = partyRepository.findById(id).orElseThrow();
        positionRepository.deleteByParty(party);
        partyRepository.delete(party);
    }

    /** 파티 → DTO 변환 */
    private PartyDTO toDTO(PartyEntity entity) {
        PartyDTO dto = new PartyDTO();
        dto.setPartySeq(entity.getPartySeq());
        dto.setPartyName(entity.getPartyName());
        dto.setPartyType(entity.getPartyType());
        dto.setPartyCreateDate(entity.getPartyCreateDate());
        dto.setPartyEndTime(entity.getPartyEndTime());
        dto.setPartyStatus(entity.getPartyStatus());
        dto.setPartyHeadcount(entity.getPartyHeadcount());
        dto.setPartyMax(entity.getPartyMax());
        dto.setMemo(entity.getMemo());
        dto.setMainPosition(entity.getMainPosition());
        dto.setPositions(
                positionRepository.findByParty(entity).stream()
                        .map(PartyFindPositionEntity::getPosition)
                        .collect(Collectors.toList())
        );
        return dto;
    }

    /** DTO → 파티 엔티티 변환 (생성 시 사용) */
    private PartyEntity toEntity(PartyDTO dto) {
        PartyEntity entity = new PartyEntity();
        entity.setPartyName(dto.getPartyName());
        entity.setPartyType(dto.getPartyType());
        entity.setPartyEndTime(dto.getPartyEndTime());
        entity.setPartyStatus(dto.getPartyStatus());
        entity.setPartyHeadcount(dto.getPartyHeadcount());
        entity.setPartyMax(dto.getPartyMax());
        entity.setMemo(dto.getMemo());
        entity.setMainPosition(dto.getMainPosition());
        return entity;
    }

    /** 모집 포지션 저장 */
    private void savePositions(PartyEntity party, List<PositionEnum> positions) {
        if (positions != null) {
            for (PositionEnum pos : positions) {
                PartyFindPositionEntity p = new PartyFindPositionEntity();
                p.setParty(party);
                p.setPosition(pos);
                positionRepository.save(p);
            }
        }
    }
}