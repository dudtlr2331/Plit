package com.plit.FO.clan.service;

import com.plit.FO.clan.dto.ClanMemberDTO;
import com.plit.FO.clan.entity.ClanEntity;
import com.plit.FO.clan.entity.ClanMemberEntity;
import com.plit.FO.clan.repository.ClanMemberRepository;
import com.plit.FO.clan.repository.ClanRepository;
import com.plit.FO.clan.enums.Position;
import com.plit.FO.user.entity.UserEntity;
import com.plit.FO.user.repository.UserRepository;
import com.plit.FO.user.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClanMemberServiceImpl implements ClanMemberService {

    private final ClanMemberRepository clanMemberRepository;
    private final UserService userService;
    private final ClanRepository clanRepository;
    private final UserRepository userRepository;

    @Override
    public int countByClanId(Long clanId) {
        return clanMemberRepository.countByClanId(clanId);
    }

    @Override
    public List<ClanMemberDTO> findApprovedMembersByClanId(Long clanId) {
        return clanMemberRepository.findByClanIdAndStatus(clanId, "APPROVED")
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ClanMemberDTO> findPendingMembersByClanId(Long clanId) {
        return clanMemberRepository.findByClanIdAndStatus(clanId, "PENDING")
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private ClanMemberDTO convertToDTO(ClanMemberEntity entity) {
        ClanMemberDTO dto = new ClanMemberDTO();
        dto.setMemberId(entity.getUserId());
        dto.setRole(entity.getRole());
        dto.setStatus(entity.getStatus());
        dto.setJoinedAt(entity.getJoinedAt());
        dto.setPosition(entity.getPosition() != null ? entity.getPosition() : Position.ALL);
        dto.setJoinedAgo(formatJoinedAgo(entity.getJoinedAt()));

        userService.getUserBySeq(entity.getUserId().intValue()).ifPresent(user -> {
            dto.setNickname(user.getUserNickname());
            dto.setUserId(user.getUserId());
        });

        dto.setTier(entity.getTier() != null ? entity.getTier() : "Unranked");
        dto.setIntro(entity.getIntro() != null ? entity.getIntro() : "소개글이 없습니다.");

        return dto;
    }
    @Override
    public Optional<ClanMemberDTO> findByClanIdAndUserId(Long clanId, Long userId) {
        return clanMemberRepository.findByClanIdAndUserId(clanId, userId)
                .map(this::convertToDTO);
    }

    @Override
    @Transactional
    public void updateMemberInfo(Long userId, Long clanId, ClanMemberDTO dto) {
        ClanMemberEntity entity = clanMemberRepository.findByClanIdAndUserId(clanId, userId)
                .orElseThrow(() -> new RuntimeException("해당 멤버를 찾을 수 없습니다."));

        entity.setPosition(dto.getPosition());
        entity.setIntro(dto.getIntro());
    }

    @Override
    @Transactional
    public void addMember(Long clanId, Long userId, String position, String tier, String intro) {
        ClanMemberEntity entity = ClanMemberEntity.builder()
                .clanId(clanId)
                .userId(userId)
                .position(Position.valueOf(position))
                .tier(tier)
                .intro(intro)
                .status("APPROVED")
                .role("MEMBER")
                .build();

        clanMemberRepository.save(entity);
    }

    @Override
    @Transactional
    public void delegateLeader(Long clanId, Long fromUserSeq, Long toUserSeq) {
        if (fromUserSeq.equals(toUserSeq)) {
            throw new IllegalArgumentException("본인에게는 위임할 수 없습니다.");
        }

        ClanMemberEntity leader = clanMemberRepository.findByClanIdAndUserId(clanId, fromUserSeq)
                .orElseThrow(() -> new RuntimeException("현재 리더를 찾을 수 없습니다."));
        if (!"LEADER".equals(leader.getRole())) {
            throw new RuntimeException("리더만 위임할 수 있습니다.");
        }

        ClanMemberEntity target = clanMemberRepository.findByClanIdAndUserId(clanId, toUserSeq)
                .orElseThrow(() -> new RuntimeException("위임 대상 멤버가 없습니다."));

        ClanEntity clan = clanRepository.findById(clanId)
                .orElseThrow(() -> new RuntimeException("클랜을 찾을 수 없습니다."));

        leader.setRole("MEMBER");
        target.setRole("LEADER");
        clan.setLeaderId(toUserSeq);
    }

    @Override
    @Transactional
    public void kickMember(Long clanId, Long requesterUserSeq, Long targetUserSeq) {
        // 요청자 리더인지 확인
        ClanEntity clan = clanRepository.findById(clanId)
                .orElseThrow(() -> new RuntimeException("클랜을 찾을 수 없습니다."));

        if (!clan.getLeaderId().equals(requesterUserSeq)) {
            throw new RuntimeException("리더만 추방할 수 있습니다.");
        }

        if (requesterUserSeq.equals(targetUserSeq)) {
            throw new RuntimeException("자기 자신은 추방할 수 없습니다.");
        }

        ClanMemberEntity targetMember = clanMemberRepository.findByClanIdAndUserId(clanId, targetUserSeq)
                .orElseThrow(() -> new RuntimeException("해당 멤버를 찾을 수 없습니다."));

        clanMemberRepository.delete(targetMember);
    }

    @Override
    public void leaveClan(Long clanId, Long userSeq) {

        ClanMemberEntity member = clanMemberRepository
                .findByClanIdAndUserId(clanId, userSeq)
                .orElseThrow(() -> new RuntimeException("클랜 멤버가 아닙니다."));

        clanMemberRepository.delete(member);
    }

    @Override
    public int countPendingMembers(Long clanId) {
        return clanMemberRepository.countByClanIdAndStatus(clanId, "PENDING");
    }

    private String formatJoinedAgo(LocalDateTime joinedAt) {
        if (joinedAt == null) return "";
        Duration duration = Duration.between(joinedAt, LocalDateTime.now());

        long days = duration.toDays();
        if (days > 0) return days + "일 전";

        long hours = duration.toHours();
        if (hours > 0) return hours + "시간 전";

        long minutes = duration.toMinutes();
        if (minutes > 0) return minutes + "분 전";

        return "방금 전";
    }
}