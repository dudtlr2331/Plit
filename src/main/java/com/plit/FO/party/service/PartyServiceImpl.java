package com.plit.FO.party.service;

import com.plit.FO.party.dto.PartyMemberDTO;
import com.plit.FO.party.enums.MemberStatus;
import com.plit.FO.party.enums.PartyStatus;
import com.plit.FO.party.enums.PositionEnum;
import com.plit.FO.party.dto.PartyDTO;
import com.plit.FO.party.entity.PartyEntity;
import com.plit.FO.party.entity.PartyFindPositionEntity;
import com.plit.FO.party.entity.PartyMemberEntity;
import com.plit.FO.party.repository.PartyFindPositionRepository;
import com.plit.FO.party.repository.PartyMemberRepository;
import com.plit.FO.party.repository.PartyRepository;
import com.plit.FO.user.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
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
    private final UserRepository userRepository;

    public PartyServiceImpl(PartyRepository partyRepository, PartyFindPositionRepository positionRepository, PartyFindPositionRepository partyFindPositionRepository, PartyMemberRepository partyMemberRepository, UserRepository userRepository) {
        this.partyRepository = partyRepository;
        this.positionRepository = positionRepository;
        this.partyFindPositionRepository = partyFindPositionRepository;
        this.partyMemberRepository = partyMemberRepository;
        this.userRepository = userRepository;
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
        int calculatedMax;

        if ("solo".equals(dto.getPartyType())) {
            calculatedMax = 2; // 파티장 + 1명
        } else {
            calculatedMax = (dto.getPositions() != null && dto.getPositions().contains("ALL"))
                    ? 5
                    : Math.min((dto.getPositions() != null ? dto.getPositions().size() : 0) + 1, 5);
        }

        // 종료일이 현재보다 과거인 경우 예외 처리
        if (dto.getPartyEndTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("종료일은 현재보다 미래여야 합니다.");
        }

        PartyEntity party = PartyEntity.builder()
                .partyName(dto.getPartyName())
                .partyType(dto.getPartyType())
                .partyStatus(PartyStatus.valueOf(dto.getPartyStatus()))
                .partyCreateDate(LocalDateTime.now())
                .partyEndTime(dto.getPartyEndTime())
                .partyHeadcount(1)
                .partyMax(calculatedMax)
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

        PartyMemberEntity leader = PartyMemberEntity.builder()
                .party(party)
                .userId(userId)
                .position(dto.getMainPosition())
                .status(MemberStatus.ACCEPTED)
                .message("파티장")
                .build();

        partyMemberRepository.save(leader);
    }

    @Override
    @Transactional
    public void updateParty(Long id, PartyDTO dto) {
        // 종료일이 지난 파티는 재모집이 안되도록 설정
        if (dto.getPartyEndTime().isBefore(LocalDateTime.now()) && PartyStatus.valueOf(dto.getPartyStatus()) == PartyStatus.WAITING) {
            throw new IllegalArgumentException("종료된 파티는 다시 모집할 수 없습니다.");
        }

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

        if (party.getPartyStatus() == PartyStatus.FULL || party.getPartyStatus() == PartyStatus.CLOSED) {
            throw new IllegalStateException("마감된 파티에는 참가할 수 없습니다.");
        }

        if (partyMemberRepository.existsByParty_PartySeqAndUserId(partySeq, userId)) {
            throw new IllegalStateException("이미 참가한 파티입니다.");
        }

        // 인원수 조건은 현재 ACCEPTED 된 멤버만 체크
        int acceptedCount = partyMemberRepository.countByParty_PartySeqAndStatus(partySeq, MemberStatus.ACCEPTED);
        if (acceptedCount >= party.getPartyMax()) {
            throw new IllegalStateException("파티 인원이 가득 찼습니다.");
        }

        // PENDING 상태로 저장
        PartyMemberEntity member = PartyMemberEntity.builder()
                .party(party)
                .userId(userId)
                .role("MEMBER")
                .message(message)
                .status(MemberStatus.PENDING)
                .position(position)
                .build();

        partyMemberRepository.save(member);
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
        return partyMemberRepository.findByParty_PartySeqAndStatus(partySeq, MemberStatus.ACCEPTED).stream()
                .map(PartyMemberEntity::getUserId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void acceptMember(Long partyId, Long memberId) {
        PartyMemberEntity member = partyMemberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("멤버가 존재하지 않습니다."));

        PartyEntity party = member.getParty();

        if (!party.getPartySeq().equals(partyId)) {
            throw new IllegalArgumentException("파티 불일치");
        }

        // 최대 인원 수 제한
        if (party.getPartyHeadcount() >= party.getPartyMax()) {
            throw new IllegalStateException("최대 인원을 초과할 수 없습니다.");
        }

        // 포지션 중복 수락 제한 (단, ALL은 중복 허용)
        List<PartyMemberEntity> acceptedMembers = partyMemberRepository.findByParty_PartySeqAndStatus(partyId, MemberStatus.ACCEPTED);
        boolean isAllPosition = "ALL".equalsIgnoreCase(member.getPosition());

        if (!isAllPosition) {
            boolean positionTaken = acceptedMembers.stream()
                    .anyMatch(m -> m.getPosition().equalsIgnoreCase(member.getPosition()));
            if (positionTaken) {
                throw new IllegalStateException("해당 포지션은 이미 다른 참가자가 수락되었습니다.");
            }
        }

        member.setStatus(MemberStatus.ACCEPTED);
        party.setPartyHeadcount(party.getPartyHeadcount() + 1);

        partyMemberRepository.save(member);
        partyRepository.save(party);
    }

    @Transactional
    public void rejectMember(Long partyId, Long memberId) {
        PartyMemberEntity member = partyMemberRepository.findById(memberId).orElseThrow();
        if (!member.getParty().getPartySeq().equals(partyId)) throw new IllegalArgumentException("파티 불일치");
        member.setStatus(MemberStatus.REJECTED);
        partyMemberRepository.save(member);
    }

    @Override
    public List<PartyMemberDTO> getPartyMemberDTOs(Long partySeq) {
        PartyEntity party = partyRepository.findById(partySeq).orElseThrow();

        return partyMemberRepository.findByParty(party).stream()
                .map(member -> {
                    Integer userSeq = userRepository.findByUserNickname(member.getUserId())
                            .map(user -> user.getUserSeq())
                            .orElse(null); // 없으면 null로 설정
                    return new PartyMemberDTO(member, userSeq);
                })
                .collect(Collectors.toList());
    }

    @Override
    public MemberStatus getJoinStatus(Long partyId, String userId) {
        return partyMemberRepository.findByParty_PartySeqAndUserId(partyId, userId)
                .map(PartyMemberEntity::getStatus)
                .orElse(null); // 또는 Optional<MemberStatus>로 감싸도 OK
    }

    @Override
    public boolean existsByParty_PartySeqAndStatusAndPosition(Long partySeq, String status, String position) {
        return partyMemberRepository.existsByParty_PartySeqAndStatusAndPosition(partySeq, status, position);
    }

    @Override
    public void kickMember(Long partyId, Long memberId, String requesterId) {
        PartyEntity party = partyRepository.findById(partyId)
                .orElseThrow(() -> new RuntimeException("파티가 존재하지 않습니다."));

        if (!party.getCreatedBy().equals(requesterId)) {
            throw new AccessDeniedException("파티장만 내보내기를 할 수 있습니다.");
        }

        PartyMemberEntity member = partyMemberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("멤버가 존재하지 않습니다."));

        if (!member.getParty().getPartySeq().equals(partyId)) {
            throw new IllegalArgumentException("해당 파티의 멤버가 아닙니다.");
        }

        if (member.getStatus() != MemberStatus.ACCEPTED) {
            throw new IllegalStateException("수락된 멤버만 내보낼 수 있습니다.");
        }

        partyMemberRepository.delete(member);

        // 인원수 업데이트 (필요시)
        party.setPartyHeadcount(party.getPartyHeadcount() - 1);
        partyRepository.save(party);
    }

    /* 파티 나가기 */
    @Override
    @Transactional
    public void leaveParty(Long partyId, String userId) {
        PartyMemberEntity member = partyMemberRepository
                .findByParty_PartySeqAndUserId(partyId, userId)
                .orElseThrow(() -> new IllegalStateException("파티 참가 기록이 없습니다."));

        if (member.getStatus() != MemberStatus.ACCEPTED) {
            throw new IllegalStateException("수락된 멤버만 탈퇴할 수 있습니다.");
        }

        partyMemberRepository.delete(member);

        PartyEntity party = member.getParty();
        party.setPartyHeadcount(party.getPartyHeadcount() - 1);
        partyRepository.save(party);
    }
}
