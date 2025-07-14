package com.plit.FO.clan.service;

import com.plit.FO.clan.dto.ClanMemberDTO;
import com.plit.FO.clan.entity.ClanMemberEntity;
import com.plit.FO.clan.repository.ClanMemberRepository;
import com.plit.FO.user.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClanMemberServiceImpl implements ClanMemberService {

    private final ClanMemberRepository clanMemberRepository;
    private final UserService userService;

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
        dto.setMainPosition(entity.getMainPosition());

        userService.getUserBySeq(entity.getUserId().intValue()).ifPresent(user -> {
            dto.setNickname(user.getUserNickname());
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

        entity.setMainPosition(dto.getMainPosition());
        entity.setIntro(dto.getIntro());
    }

    @Override
    @Transactional
    public void addMember(Long clanId, Long userId, String position, String tier, String intro) {
        ClanMemberEntity entity = ClanMemberEntity.builder()
                .clanId(clanId)
                .userId(userId)
                .mainPosition(position)
                .tier(tier)
                .intro(intro)
                .status("APPROVED") // 승인 상태
                .role("MEMBER")     // 기본 멤버
                .build();

        clanMemberRepository.save(entity);
    }

}