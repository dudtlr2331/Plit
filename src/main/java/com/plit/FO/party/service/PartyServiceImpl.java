package com.plit.FO.party.service;

import com.plit.FO.party.enums.PartyStatus;
import com.plit.FO.party.enums.PositionEnum;
import com.plit.FO.party.dto.PartyDTO;
import com.plit.FO.party.entity.PartyEntity;
import com.plit.FO.party.entity.PartyFindPositionEntity;
import com.plit.FO.party.entity.PartyMemberEntity;
import com.plit.FO.party.repository.PartyFindPositionRepository;
import com.plit.FO.party.repository.PartyMemberRepository;
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
    private final PartyMemberRepository partyMemberRepository;

    public PartyServiceImpl(PartyRepository partyRepository, PartyFindPositionRepository positionRepository, PartyFindPositionRepository partyFindPositionRepository, PartyMemberRepository partyMemberRepository) {
        this.partyRepository = partyRepository;
        this.positionRepository = positionRepository;
        this.partyFindPositionRepository = partyFindPositionRepository;
        this.partyMemberRepository = partyMemberRepository;
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
                .partyStatus(PartyStatus.valueOf(dto.getPartyStatus()))
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
        party.setPartyStatus(PartyStatus.valueOf(dto.getPartyStatus()));
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
        dto.setPartyStatus(entity.getPartyStatus().name());
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
        entity.setPartyStatus(PartyStatus.valueOf(dto.getPartyStatus()));
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
                        .position(PositionEnum.valueOf(posStr))
                        .build())
                .collect(Collectors.toList());

        positionRepository.saveAll(entities);
    }

    @Transactional
    @Override
    public void joinParty(Long partySeq, String userId, String position, String message) {
        PartyEntity party = partyRepository.findById(partySeq)
                .orElseThrow(() -> new IllegalArgumentException("해당 파티가 존재하지 않습니다."));

        if (!"team".equals(party.getPartyType())) {
            throw new IllegalStateException("자유랭크 파티만 참가할 수 있습니다.");
        }

        if (party.getPartyStatus() == PartyStatus.FULL || party.getPartyStatus() == PartyStatus.CLOSED) {
            throw new IllegalStateException("마감된 파티에는 참가할 수 없습니다.");
        }

        if (partyMemberRepository.existsByParty_PartySeqAndUserId(partySeq, userId)) {
            throw new IllegalStateException("이미 참가한 파티입니다.");
        }

        if (party.getPartyHeadcount() >= party.getPartyMax()) {
            throw new IllegalStateException("파티 인원이 가득 찼습니다.");
        }

        // 파티 멤버 등록
        PartyMemberEntity member = PartyMemberEntity.builder()
                .party(party)
                .userId(userId)
                .role("MEMBER")
                .message(message)
                .build();

        partyMemberRepository.save(member);
        partyMemberRepository.flush();  // ★ 강제 flush

        System.out.println(">>> [Join] member 저장됨: " + member.getId());

        // 현재 인원수 증가
        party.setPartyHeadcount(party.getPartyHeadcount() + 1);

        if (party.getPartyHeadcount() >= party.getPartyMax()) {
            party.setPartyStatus(PartyStatus.FULL);
        }

        partyRepository.save(party);
    }

    @Transactional
    public String tryJoinParty(Long partySeq, String userId) {
        PartyEntity party = partyRepository.findById(partySeq)
                .orElse(null);

        if (party == null) return "해당 파티가 존재하지 않습니다.";
        if (!"team".equals(party.getPartyType())) return "자유랭크 파티만 참가할 수 있습니다.";
        if (party.getPartyStatus() == PartyStatus.FULL || party.getPartyStatus() == PartyStatus.CLOSED)
            return "마감된 파티에는 참가할 수 없습니다.";
        if (partyMemberRepository.existsByParty_PartySeqAndUserId(partySeq, userId))
            return "이미 참가한 파티입니다.";
        if (party.getPartyHeadcount() >= party.getPartyMax())
            return "파티 인원이 가득 찼습니다.";

        // 등록
        PartyMemberEntity member = PartyMemberEntity.builder()
                .party(party)
                .userId(userId)
                .role("MEMBER")
                .build();
        partyMemberRepository.save(member);

        // 인원 증가 + 상태 변경
        party.setPartyHeadcount(party.getPartyHeadcount() + 1);
        if (party.getPartyHeadcount() >= party.getPartyMax()) {
            party.setPartyStatus(PartyStatus.FULL);
        }

        partyRepository.save(party);
        return "OK";
    }

    @Override
    public List<String> getPartyMembers(Long partySeq) {
        return partyMemberRepository.findByParty_PartySeq(partySeq).stream()
                .map(PartyMemberEntity::getUserId)
                .collect(Collectors.toList());
    }
}
