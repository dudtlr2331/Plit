package com.plit.FO.clan.service;

import com.plit.FO.clan.dto.ClanJoinRequestDTO;
import com.plit.FO.clan.entity.ClanEntity;
import com.plit.FO.clan.entity.ClanJoinRequestEntity;
import com.plit.FO.clan.enums.JoinStatus;
import com.plit.FO.clan.enums.Position;
import com.plit.FO.clan.repository.ClanJoinRequestRepository;
import com.plit.FO.clan.repository.ClanRepository;
import com.plit.FO.party.enums.PositionEnum;
import com.plit.FO.user.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClanJoinRequestServiceImpl implements ClanJoinRequestService {

    private final ClanJoinRequestRepository joinRequestRepository;
    private final ClanRepository clanRepository;
    private final UserService userService;
    private final ClanMemberService clanMemberService;

    @Override
    public void requestJoin(ClanJoinRequestDTO dto) {
        Optional<ClanJoinRequestEntity> existingRequestOpt =
                joinRequestRepository.findByClanIdAndUserIdAndStatus(dto.getClanId(), dto.getUserId(), JoinStatus.REJECTED);

        if (existingRequestOpt.isPresent()) {
            ClanJoinRequestEntity existingRequest = existingRequestOpt.get();

            if (existingRequest.getStatus() == JoinStatus.REJECTED) {
                existingRequest.setStatus(JoinStatus.PENDING);
                existingRequest.setIntro(dto.getIntro());
                existingRequest.setTier(dto.getTier());
                existingRequest.setPosition(dto.getPosition());
                existingRequest.setRequestAt(LocalDateTime.now());

                joinRequestRepository.save(existingRequest);
                return;
            }

            throw new IllegalStateException("이미 신청되어 있습니다.");
        }

        ClanEntity clan = clanRepository.findById(dto.getClanId())
                .orElseThrow(() -> new IllegalArgumentException("클랜을 찾을 수 없습니다."));

        ClanJoinRequestEntity entity = ClanJoinRequestEntity.builder()
                .clan(clan)
                .userId(dto.getUserId())
                .position(dto.getPosition())
                .intro(dto.getIntro())
                .tier(dto.getTier())
                .requestAt(LocalDateTime.now())
                .status(JoinStatus.PENDING)
                .build();

        joinRequestRepository.save(entity);
    }

    @Override
    public List<ClanJoinRequestDTO> getJoinRequests(Long clanId) {
        return joinRequestRepository.findByClan_IdAndStatus(clanId, JoinStatus.PENDING)
                .stream()
                .map(req -> {
                    ClanJoinRequestDTO dto = new ClanJoinRequestDTO();
                    dto.setClanId(clanId);
                    dto.setUserId(req.getUserId());
                    dto.setPosition(req.getPosition());
                    dto.setIntro(req.getIntro());
                    dto.setRequestAt(req.getRequestAt());

                    userService.getUserBySeq(req.getUserId().intValue()).ifPresent(user -> {
                        dto.setNickname(user.getUserNickname());
                        dto.setTier(req.getTier());
                    });

                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean isJoinPending(Long clanId, Long userId) {
        return joinRequestRepository.existsByClanIdAndUserIdAndStatus(clanId, userId, JoinStatus.PENDING);
    }

    @Override
    @Transactional
    public void approveJoinRequest(Long clanId, Long userId) {
        ClanJoinRequestEntity request = joinRequestRepository
                .findByClanIdAndUserIdAndStatus(clanId, userId, JoinStatus.PENDING)
                .orElseThrow(() -> new IllegalArgumentException("가입 신청을 찾을 수 없습니다."));

        request.setStatus(JoinStatus.APPROVED);
        joinRequestRepository.save(request);

        String position = request.getPosition().name();
        String tier = request.getTier();
        String intro = request.getIntro();

        clanMemberService.addMember(clanId, userId, position, tier, intro);
    }

    @Override
    public void rejectJoinRequest(Long clanId, Long userId) {
        ClanJoinRequestEntity request = joinRequestRepository
                .findByClanIdAndUserIdAndStatus(clanId, userId, JoinStatus.PENDING)
                .orElseThrow(() -> new IllegalArgumentException("가입 신청을 찾을 수 없습니다."));

        request.setStatus(JoinStatus.REJECTED);
        joinRequestRepository.save(request);
    }
}
