package com.plit.FO.clan.service;

import com.plit.FO.clan.dto.ClanJoinRequestDTO;
import com.plit.FO.clan.entity.ClanEntity;
import com.plit.FO.clan.entity.ClanJoinRequestEntity;
import com.plit.FO.clan.enums.JoinStatus;
import com.plit.FO.clan.enums.Position;
import com.plit.FO.clan.repository.ClanJoinRequestRepository;
import com.plit.FO.clan.repository.ClanRepository;
import com.plit.FO.matchHistory.dto.riot.RiotSummonerResponse;
import com.plit.FO.matchHistory.repository.MatchOverallSummaryRepository;
import com.plit.FO.matchHistory.service.ImageService;
import com.plit.FO.matchHistory.service.RiotApiService;
import com.plit.FO.user.entity.UserEntity;
import com.plit.FO.user.repository.UserRepository;
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

    private final RiotApiService riotApiService;
    private final ClanJoinRequestRepository joinRequestRepository;
    private final ClanRepository clanRepository;
    private final ClanMemberService clanMemberService;
    private final MatchOverallSummaryRepository matchOverallSummaryRepository;
    private final ImageService imageService;
    private final UserRepository userRepository;


    @Override
    public void requestJoin(ClanJoinRequestDTO dto) {
        try {
            if (dto == null || dto.getClanId() == null || dto.getUserId() == null) {
                throw new IllegalArgumentException("가입 요청 정보가 유효하지 않습니다.");
            }

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
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("클랜 가입 요청 처리 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public List<ClanJoinRequestDTO> getJoinRequests(Long clanId) {
        try {
            if (clanId == null) {
                throw new IllegalArgumentException("클랜 ID는 null일 수 없습니다.");
            }

            return joinRequestRepository.findByClan_IdAndStatus(clanId, JoinStatus.PENDING)
                    .stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            throw new RuntimeException("가입 요청 목록 조회 중 오류가 발생했습니다.", e);
        }
    }

    private ClanJoinRequestDTO convertToDTO(ClanJoinRequestEntity entity) {
        ClanJoinRequestDTO dto = new ClanJoinRequestDTO();

        try {
            dto.setClanId(entity.getClan().getId());
            dto.setUserId(entity.getUserId());
            dto.setPosition(entity.getPosition() != null ? entity.getPosition() : Position.ALL);
            dto.setIntro(entity.getIntro());
            dto.setRequestAt(entity.getRequestAt());

            System.out.println("member.position = " + dto.getPosition());

            UserEntity user = userRepository.findById(entity.getUserId().intValue()).orElse(null);
            if (user == null) {
                throw new IllegalStateException("유저 정보를 찾을 수 없습니다.");
            }

            dto.setNickname(user.getUserNickname());
            dto.setUserId(user.getUserSeq().longValue());
            dto.setTier(entity.getTier());

            if (user.getPuuid() != null && !user.getPuuid().isEmpty()) {
                matchOverallSummaryRepository.findByPuuid(user.getPuuid()).ifPresentOrElse(summary -> {
                    var summaryDto = summary.toDTO();

                    dto.setPreferredChampions(String.join(", ", summaryDto.getPreferredChampions()));
                    dto.setWinRate(summaryDto.getWinRate());

                    List<String> championImageUrls = summaryDto.getPreferredChampions().stream()
                            .map(String::trim)
                            .map(name -> imageService.getImageUrl(name + ".png", "champion"))
                            .toList();
                    dto.setChampionImageUrls(championImageUrls);

                    dto.setTotalWins(summaryDto.getTotalWins());
                    dto.setTotalLosses(summaryDto.getTotalMatches() - summaryDto.getTotalWins());

                    dto.setAverageKills(summaryDto.getAverageKills());
                    dto.setAverageDeaths(summaryDto.getAverageDeaths());
                    dto.setAverageAssists(summaryDto.getAverageAssists());
                    dto.setAverageKda(summaryDto.getAverageKda());

                    String tier = summaryDto.getTier();
                    dto.setTier(tier != null && !tier.isBlank() ? tier : "Unranked");

                    if (tier != null && !tier.isBlank()) {
                        String baseTier = tier.replaceAll("[^A-Za-z]", "").toUpperCase();
                        String tierNum = tier.replaceAll("[^0-9]", "");

                        dto.setTierImageUrl("/images/tier/" + baseTier + ".png");

                        String shortCode = switch (baseTier) {
                            case "CHALLENGER" -> "C1";
                            case "GRANDMASTER" -> "GM1";
                            case "MASTER" -> "M1";
                            default -> baseTier.charAt(0) + tierNum;
                        };
                        dto.setTierShort(shortCode);
                    } else {
                        dto.setTierImageUrl(null);
                        dto.setTierShort("Unranked");
                    }

                    try {
                        RiotSummonerResponse response = riotApiService.getSummonerByPuuid(user.getPuuid());
                        if (response != null) {
                            String profileIconUrl = imageService.getProfileIconUrl(response.getProfileIconId());
                            dto.setProfileIconUrl(profileIconUrl);

                        }
                    } catch (Exception e) {
                        System.err.println("소환사 정보 불러오기 실패: " + e.getMessage());
                    }

                }, () -> {
                    System.out.println("MatchOverallSummary 없음");
                    dto.setPreferredChampions(null);
                    dto.setChampionImageUrls(null);
                    dto.setWinRate(null);
                    dto.setAverageKda(null);
                });

            } else {
                System.out.println("puuid 없음 → Riot 요약 정보 비움");
                dto.setPreferredChampions(null);
                dto.setChampionImageUrls(null);
                dto.setWinRate(null);
                dto.setAverageKda(null);
            }

        } catch (Exception e) {
            System.err.println("ClanJoinRequestDTO 변환 중 오류: " + e.getMessage());
        }

        if (dto.getProfileIconUrl() == null || dto.getProfileIconUrl().isBlank()) {
            dto.setProfileIconUrl("/images/clan/clan_default.png");
        }

        return dto;
    }

    @Override
    public boolean isJoinPending(Long clanId, Long userId) {
        if (clanId == null || userId == null) {
            throw new IllegalArgumentException("클랜 ID와 유저 ID는 null일 수 없습니다.");
        }

        try {
            return joinRequestRepository.existsByClanIdAndUserIdAndStatus(clanId, userId, JoinStatus.PENDING);
        } catch (Exception e) {
            System.err.println("가입 대기 여부 확인 중 오류 발생: " + e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public void approveJoinRequest(Long clanId, Long userId) {
        if (clanId == null || userId == null) {
            throw new IllegalArgumentException("클랜 ID와 유저 ID는 null일 수 없습니다.");
        }

        try {
            ClanJoinRequestEntity request = joinRequestRepository
                    .findByClanIdAndUserIdAndStatus(clanId, userId, JoinStatus.PENDING)
                    .orElseThrow(() -> new IllegalArgumentException("가입 신청을 찾을 수 없습니다."));

            request.setStatus(JoinStatus.APPROVED);
            joinRequestRepository.save(request);

            String position = request.getPosition() != null ? request.getPosition().name() : Position.ALL.name();
            String tier = request.getTier();
            String intro = request.getIntro();

            clanMemberService.addMember(clanId, userId, position, tier, intro);

        } catch (Exception e) {
            throw new RuntimeException("가입 수락 처리 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public void rejectJoinRequest(Long clanId, Long userId) {
        if (clanId == null || userId == null) {
            throw new IllegalArgumentException("클랜 ID와 유저 ID는 null일 수 없습니다.");
        }

        try {
            ClanJoinRequestEntity request = joinRequestRepository
                    .findByClanIdAndUserIdAndStatus(clanId, userId, JoinStatus.PENDING)
                    .orElseThrow(() -> new IllegalArgumentException("가입 신청을 찾을 수 없습니다."));

            request.setStatus(JoinStatus.REJECTED);
            joinRequestRepository.save(request);

        } catch (Exception e) {
            throw new RuntimeException("가입 거절 처리 중 오류가 발생했습니다.", e);
        }
    }
}
