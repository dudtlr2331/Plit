package com.plit.FO.party;

import org.springframework.stereotype.Service;

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

    public List<PartyDTO> getAllParties() {
        return partyRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public PartyDTO getParty(Long id) {
        PartyEntity party = partyRepository.findById(id).orElseThrow();
        return toDTO(party);
    }

    public void saveParty(PartyDTO dto) {
        PartyEntity party = toEntity(dto);
        party.setPartyCreateDate(LocalDateTime.now());
        PartyEntity saved = partyRepository.save(party);

        savePositions(saved, dto.getPositions());
    }

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

        partyRepository.save(party);

        positionRepository.deleteByParty(party);
        savePositions(party, dto.getPositions());
    }

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
        dto.setPositions(positionRepository.findByParty(entity).stream()
                .map(PartyFindPositionEntity::getPosition)
                .collect(Collectors.toList()));
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
        return entity;
    }
}