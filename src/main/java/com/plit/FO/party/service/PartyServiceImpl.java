package com.plit.FO.party.service;

import com.plit.FO.party.PositionEnum;
import com.plit.FO.party.dto.PartyDTO;
import com.plit.FO.party.dto.PartyFindPositionDTO;
import com.plit.FO.party.entity.PartyEntity;
import com.plit.FO.party.entity.PartyFindPositionEntity;
import com.plit.FO.party.repository.PartyFindPositionRepository;
import com.plit.FO.party.repository.PartyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PartyServiceImpl implements PartyService {

    private final PartyRepository partyRepository;
    private final PartyFindPositionRepository positionRepository;
    private final PartyFindPositionRepository partyFindPositionRepository;

    public PartyServiceImpl(PartyRepository partyRepository, PartyFindPositionRepository positionRepository, PartyFindPositionRepository partyFindPositionRepository) {
        this.partyRepository = partyRepository;
        this.positionRepository = positionRepository;
        this.partyFindPositionRepository = partyFindPositionRepository;
    }

    @Override
    public List<PartyDTO> findByPartyType(String partyType) {
        return partyRepository.findByPartyType(partyType).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public PartyDTO getParty(Long id) {
        PartyEntity party = partyRepository.findById(id).orElseThrow();
        return toDTO(party);
    }

    @Override
    @Transactional
    public void saveParty(PartyDTO dto, String userId) {
        PartyEntity party = PartyEntity.builder()
                .partyName(dto.getPartyName())
                .partyType(dto.getPartyType())
                .partyStatus(dto.getPartyStatus())
                .partyCreateDate(LocalDateTime.now())
                .partyEndTime(dto.getPartyEndTime())
                .partyHeadcount(dto.getPartyHeadcount())
                .partyMax(dto.getPartyMax())
                .memo(dto.getMemo())
                .mainPosition(dto.getMainPosition())
                .createdBy(userId)
                .build();

        partyRepository.save(party);

        if (dto.getPositions() != null) {
            List<PartyFindPositionEntity> positionEntities = dto.getPositions().stream()
                    .map(pos -> PartyFindPositionEntity.builder()
                            .party(party)
                            .position(PositionEnum.valueOf(pos))
                            .build())
                    .toList();

            partyFindPositionRepository.saveAll(positionEntities);
        }
    }

    @Override
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

        positionRepository.deleteByParty(party);
        savePositions(party, dto.getPositions());
    }

    @Override
    @Transactional
    public void deleteParty(Long id) {
        PartyEntity party = partyRepository.findById(id).orElseThrow();
        positionRepository.deleteByParty(party);
        partyRepository.delete(party);
    }

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
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setPositions(
                positionRepository.findByParty(entity).stream()
                        .map(p -> p.getPosition().name()) // enum → String
                        .collect(Collectors.toList())
        );
        return dto;
    }

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
        entity.setCreatedBy(dto.getCreatedBy());
        return entity;
    }

    private void savePositions(PartyEntity party, List<String> positions) {
        List<PartyFindPositionEntity> entities = positions.stream()
                .map(posStr -> PartyFindPositionEntity.builder()
                        .party(party)
                        .position(PositionEnum.valueOf(posStr)) // 여기서 변환
                        .build())
                .collect(Collectors.toList());

        positionRepository.saveAll(entities);
    }
}
