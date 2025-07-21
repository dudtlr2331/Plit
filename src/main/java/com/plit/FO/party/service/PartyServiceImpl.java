package com.plit.FO.party.service;

import com.plit.FO.chat.entity.ChatRoomEntity;
import com.plit.FO.chat.entity.ChatRoomUserEntity;
import com.plit.FO.chat.repository.ChatRoomRepository;
import com.plit.FO.chat.repository.ChatRoomUserRepository;
import com.plit.FO.party.dto.*;
import com.plit.FO.party.enums.MemberStatus;
import com.plit.FO.party.enums.PartyStatus;
import com.plit.FO.party.enums.PositionEnum;
import com.plit.FO.party.entity.PartyEntity;
import com.plit.FO.party.entity.PartyFindPositionEntity;
import com.plit.FO.party.entity.PartyMemberEntity;
import com.plit.FO.party.repository.PartyFindPositionRepository;
import com.plit.FO.party.repository.PartyMemberRepository;
import com.plit.FO.party.repository.PartyRepository;
import com.plit.FO.user.entity.UserEntity;
import com.plit.FO.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PartyServiceImpl implements PartyService {

    private final PartyRepository partyRepository;
    private final PartyFindPositionRepository positionRepository;
    private final PartyFindPositionRepository partyFindPositionRepository;
    private final PartyMemberRepository partyMemberRepository;
    private final UserRepository userRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatRoomUserRepository chatRoomUserRepository;

    public PartyServiceImpl(PartyRepository partyRepository, PartyFindPositionRepository positionRepository, PartyFindPositionRepository partyFindPositionRepository, PartyMemberRepository partyMemberRepository, UserRepository userRepository) {
        this.partyRepository = partyRepository;
        this.positionRepository = positionRepository;
        this.partyFindPositionRepository = partyFindPositionRepository;
        this.partyMemberRepository = partyMemberRepository;
        this.userRepository = userRepository;
        this.chatRoomRepository = chatRoomRepository;
        this.chatRoomUserRepository = chatRoomUserRepository;
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

        // 채팅방 생성
        ChatRoomEntity chatRoom = ChatRoomEntity.builder()
                .chatRoomType("party")
                .chatRoomName(party.getPartyName()) // 파티 ID 기반 이름
                .chatRoomMax(party.getPartyMax())
                .chatRoomHeadcount(1) // 파티장 포함
                .chatRoomCreatedAt(LocalDateTime.now())
                .partyId(party.getPartySeq())
                .build();

        chatRoomRepository.save(chatRoom);

        // 파티장을 채팅방에 등록
        ChatRoomUserEntity leaderUser = ChatRoomUserEntity.builder()
                .chatRoom(chatRoom)
                .userId(
                        userRepository.findByUserId(userId).get().getUserSeq().longValue()
                )
                .joinedAt(LocalDateTime.now())
                .build();

        chatRoomUserRepository.save(leaderUser);

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
                .orElseThrow(() -> new NoSuchElementException("해당 멤버를 찾을 수 없습니다. ID=" + memberId));

        PartyEntity party = member.getParty();

        if (!party.getPartySeq().equals(partyId)) {
            throw new IllegalArgumentException("파티 불일치");
        }

        if (party.getPartyHeadcount() >= party.getPartyMax()) {
            throw new IllegalStateException("최대 인원을 초과할 수 없습니다.");
        }

        List<PartyMemberEntity> acceptedMembers = partyMemberRepository.findByParty_PartySeqAndStatus(partyId, MemberStatus.ACCEPTED);
        boolean isAllPosition = "ALL".equalsIgnoreCase(member.getPosition());

        // 일반 파티, 내전 모두에서 포지션 중복 체크 필요할 경우 유지
        if (!isAllPosition) {
            boolean positionTaken = acceptedMembers.stream()
                    .anyMatch(m -> m.getPosition().equalsIgnoreCase(member.getPosition()));
            if (positionTaken) {
                throw new IllegalStateException("해당 포지션은 이미 다른 참가자가 수락되었습니다.");
            }
        }

        // 상태 업데이트
        member.setStatus(MemberStatus.ACCEPTED);
        party.setPartyHeadcount(party.getPartyHeadcount() + 1);

        partyMemberRepository.save(member);
        partyRepository.save(party);

        // partyId로 채팅방 조회 (중복 방지)
        ChatRoomEntity room = chatRoomRepository.findByPartyId(party.getPartySeq())
                .orElseThrow(() -> new NoSuchElementException("채팅방 없음"));

        // userId → user_seq 조회 (userId는 닉네임으로 저장돼 있음)
        Long userSeq = userRepository.findByUserId(member.getUserId())
                .orElseThrow(() -> new NoSuchElementException("유저 없음: " + member.getUserId()))
                .getUserSeq().longValue();

        // 채팅방 입장 등록
        ChatRoomUserEntity newUser = ChatRoomUserEntity.builder()
                .chatRoom(room)
                .userId(userSeq)
                .joinedAt(LocalDateTime.now())
                .build();
        chatRoomUserRepository.save(newUser);

        // 채팅방 인원수 갱신
        room.setChatRoomHeadcount(room.getChatRoomHeadcount() + 1);
        chatRoomRepository.save(room);
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
                    Optional<UserEntity> userOpt = userRepository.findByUserId(member.getUserId());

                    Integer userSeq = userOpt.map(UserEntity::getUserSeq).orElse(null);
                    String nickname = userOpt.map(UserEntity::getUserNickname).orElse("알 수 없음");

                    return new PartyMemberDTO(member, userSeq, nickname);
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

    /* 내전 찾기*/
    @Override
    @Transactional
    public void joinScrimTeam(Long partyId, ScrimJoinRequestDTO request) {
        PartyEntity party = partyRepository.findById(partyId)
                .orElseThrow(() -> new NoSuchElementException("파티 없음"));

        if (!"scrim".equals(party.getPartyType())) {
            throw new IllegalArgumentException("스크림 전용 파티가 아닙니다.");
        }

        List<ScrimMemberDTO> team = request.getTeamMembers();
        if (team == null || team.size() != 5) {
            throw new IllegalArgumentException("5명의 팀원 정보를 입력해야 합니다.");
        }

        int acceptedCount = partyMemberRepository.countByParty_PartySeqAndStatus(partyId, MemberStatus.ACCEPTED);
        int max = party.getPartyMax();

        if (acceptedCount + team.size() > max) {
            throw new IllegalStateException("파티 정원이 초과됩니다.");
        }

        // 중복 유저 체크
        Set<String> existingUserIds = partyMemberRepository.findByParty_PartySeq(partyId).stream()
                .map(PartyMemberEntity::getUserId)
                .collect(Collectors.toSet());

        for (ScrimMemberDTO dto : team) {
            if (existingUserIds.contains(dto.getUserId())) {
                throw new IllegalArgumentException("이미 참가한 유저가 있습니다: " + dto.getUserId());
            }
        }

        Set<String> positionsInRequest = new HashSet<>();
        for (ScrimMemberDTO dto : team) {
            if (!positionsInRequest.add(dto.getPosition())) {
                throw new IllegalArgumentException("팀 내 중복 포지션입니다: " + dto.getPosition());
            }
        }

        // 채팅방 정보 미리 조회 (한 번만)
        ChatRoomEntity room = chatRoomRepository.findByPartyId(party.getPartySeq())
                .orElseThrow(() -> new NoSuchElementException("채팅방 없음"));

        for (ScrimMemberDTO dto : team) {
            UserEntity user = userRepository.findByUserNickname(dto.getUserId())
                    .orElseThrow(() -> new NoSuchElementException("유저 없음: " + dto.getUserId()));

            PartyMemberEntity member = PartyMemberEntity.builder()
                    .userId(dto.getUserId())
                    .party(party)
                    .position(dto.getPosition())
                    .status(MemberStatus.PENDING)
                    .message(request.getMessage())
                    .build();
            partyMemberRepository.save(member);
        }

        room.setChatRoomHeadcount(room.getChatRoomHeadcount() + team.size());
        chatRoomRepository.save(room);
    }

    @Override
    @Transactional
    public void createScrimParty(ScrimCreateRequestDTO request, String creatorId) {
        if (request.getTeamMembers() == null || request.getTeamMembers().size() != 5) {
            throw new IllegalArgumentException("5명의 팀원 정보를 입력해야 합니다.");
        }

        PartyEntity party = PartyEntity.builder()
                .partyName(request.getPartyName())
                .partyType("scrim")
                .partyCreateDate(LocalDateTime.now())
                .partyEndTime(request.getPartyEndTime())
                .partyStatus(PartyStatus.WAITING)
                .partyHeadcount(5)
                .partyMax(10) // 총 인원은 10명으로 고정
                .memo(request.getMemo())
                .mainPosition("ALL")
                .createdBy(creatorId)
                .build();

        Set<String> usedPositions = new HashSet<>();
        for (ScrimMemberDTO dto : request.getTeamMembers()) {
            if (!usedPositions.add(dto.getPosition())) {
                throw new IllegalArgumentException("중복된 포지션입니다: " + dto.getPosition());
            }
        }

        partyRepository.save(party);

        // 5명 멤버 등록
        for (ScrimMemberDTO dto : request.getTeamMembers()) {
            UserEntity user = userRepository.findByUserNickname(dto.getUserId())
                    .orElseThrow(() -> new NoSuchElementException("유저 없음: " + dto.getUserId()));

            PartyMemberEntity member = PartyMemberEntity.builder()
                    .userId(dto.getUserId())
                    .party(party)
                    .position(dto.getPosition())
                    .status(MemberStatus.ACCEPTED)
                    .message("내전 팀 등록")
                    .build();

            partyMemberRepository.save(member);
        }

        // 채팅방 생성
        ChatRoomEntity chatRoom = ChatRoomEntity.builder()
                .chatRoomType("party")
                .chatRoomName(party.getPartyName())
                .chatRoomMax(10)
                .chatRoomHeadcount(5)
                .chatRoomCreatedAt(LocalDateTime.now())
                .partyId(party.getPartySeq())
                .build();

        chatRoomRepository.save(chatRoom);

        // 5명 모두 채팅방에 등록
        for (ScrimMemberDTO dto : request.getTeamMembers()) {
            UserEntity user = userRepository.findByUserNickname(dto.getUserId())
                    .orElseThrow(() -> new NoSuchElementException("유저 없음: " + dto.getUserId()));

            ChatRoomUserEntity chatUser = ChatRoomUserEntity.builder()
                    .chatRoom(chatRoom)
                    .userId(user.getUserSeq().longValue())
                    .joinedAt(LocalDateTime.now())
                    .build();

            chatRoomUserRepository.save(chatUser);
        }
    }

    @Override
    @Transactional
    public void approveTeam(Long partyId, List<Long> memberIds) {
        PartyEntity party = partyRepository.findById(partyId)
                .orElseThrow(() -> new NoSuchElementException("파티 없음"));

        if (!"scrim".equals(party.getPartyType())) {
            throw new IllegalStateException("팀 수락은 내전(scrim) 파티에서만 가능합니다.");
        }

        // 채팅방 조회
        ChatRoomEntity room = chatRoomRepository.findByPartyId(partyId)
                .orElseThrow(() -> new NoSuchElementException("채팅방 없음"));

        // 1. 현재 수락된 멤버들의 포지션별 인원 수 계산
        Map<String, Long> positionCounts = partyMemberRepository.findByParty_PartySeqAndStatus(partyId, MemberStatus.ACCEPTED)
                .stream()
                .collect(Collectors.groupingBy(
                        m -> m.getPosition().toUpperCase(),
                        Collectors.counting()
                ));

        for (Long memberId : memberIds) {
            PartyMemberEntity member = partyMemberRepository.findById(memberId)
                    .orElseThrow(() -> new NoSuchElementException("멤버 없음: " + memberId));

            if (!partyId.equals(member.getParty().getPartySeq())) {
                throw new IllegalArgumentException("멤버 파티 불일치: " + memberId);
            }

            String position = member.getPosition().toUpperCase();
            long currentCount = positionCounts.getOrDefault(position, 0L);

            // "ALL" 포지션 제외하고 각 포지션 최대 2명까지 허용
            if (!"ALL".equals(position) && currentCount >= 2) {
                throw new IllegalStateException("포지션 '" + position + "'은 이미 2명이 수락되었습니다.");
            }

            // 상태 변경
            member.setStatus(MemberStatus.ACCEPTED);
            partyMemberRepository.save(member);

            // 포지션 카운트 증가
            positionCounts.put(position, currentCount + 1);

            // 채팅방 입장 처리
            Long userSeq = userRepository.findByUserNickname(member.getUserId())
                    .orElseThrow(() -> new NoSuchElementException("유저 없음: " + member.getUserId()))
                    .getUserSeq().longValue();

            ChatRoomUserEntity chatUser = ChatRoomUserEntity.builder()
                    .chatRoom(room)
                    .userId(userSeq)
                    .joinedAt(LocalDateTime.now())
                    .build();

            chatRoomUserRepository.save(chatUser);
        }

        // headcount 및 상태 갱신
        party.setPartyHeadcount(party.getPartyHeadcount() + memberIds.size());
        if (party.getPartyHeadcount() >= party.getPartyMax()) {
            party.setPartyStatus(PartyStatus.FULL);
        }

        partyRepository.save(party);

        // 채팅방 인원 갱신
        room.setChatRoomHeadcount(room.getChatRoomHeadcount() + memberIds.size());
        chatRoomRepository.save(room);
    }

    @Override
    @Transactional
    public void rejectTeam(Long partyId, List<Long> memberIds) {
        PartyEntity party = partyRepository.findById(partyId)
                .orElseThrow(() -> new NoSuchElementException("파티 없음"));

        if (!"scrim".equals(party.getPartyType())) {
            throw new IllegalStateException("팀 거절은 내전(scrim) 파티에서만 가능합니다.");
        }

        for (Long id : memberIds) {
            rejectMember(partyId, id); // 기존 거절 메서드 호출
        }
    }
}