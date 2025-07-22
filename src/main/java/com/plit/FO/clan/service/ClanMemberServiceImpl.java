package com.plit.FO.clan.service;

import com.plit.FO.clan.dto.ClanMemberDTO;
import com.plit.FO.clan.entity.ClanEntity;
import com.plit.FO.clan.entity.ClanMemberEntity;
import com.plit.FO.clan.repository.ClanMemberRepository;
import com.plit.FO.clan.repository.ClanRepository;
import com.plit.FO.clan.enums.Position;
import com.plit.FO.matchHistory.dto.riot.RiotSummonerResponse;
import com.plit.FO.matchHistory.repository.MatchOverallSummaryRepository;
import com.plit.FO.matchHistory.service.ImageService;
import com.plit.FO.matchHistory.service.RiotApiService;
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

    private final RiotApiService riotApiService;
    private final ClanMemberRepository clanMemberRepository;
    private final ClanRepository clanRepository;
    private final UserRepository userRepository;
    private final MatchOverallSummaryRepository matchOverallSummaryRepository;
    private final ImageService imageService;



    @Override
    public int countByClanId(Long clanId) {
        if (clanId == null) {
            throw new IllegalArgumentException("클랜 ID는 null일 수 없습니다.");
        }

        try {
            return clanMemberRepository.countByClanId(clanId);
        } catch (Exception e) {
            throw new RuntimeException("클랜 멤버 수 조회 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public List<ClanMemberDTO> findApprovedMembersByClanId(Long clanId) {
        if (clanId == null) {
            throw new IllegalArgumentException("클랜 ID는 null일 수 없습니다.");
        }

        try {
            return clanMemberRepository.findByClanIdAndStatus(clanId, "APPROVED")
                    .stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("클랜 멤버 목록 조회 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public List<ClanMemberDTO> findPendingMembersByClanId(Long clanId) {
        if (clanId == null) {
            throw new IllegalArgumentException("클랜 ID는 null일 수 없습니다.");
        }

        try {
            return clanMemberRepository.findByClanIdAndStatus(clanId, "PENDING")
                    .stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("클랜 대기 멤버 목록 조회 중 오류가 발생했습니다.", e);
        }
    }

    private ClanMemberDTO convertToDTO(ClanMemberEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("클랜 멤버 엔티티가 null입니다.");
        }

        ClanMemberDTO dto = new ClanMemberDTO();
        try {
            dto.setMemberId(entity.getUserId());
            dto.setRole(entity.getRole());
            dto.setStatus(entity.getStatus());
            dto.setJoinedAt(entity.getJoinedAt());
            dto.setPosition(entity.getPosition() != null ? entity.getPosition() : Position.ALL);
            dto.setJoinedAgo(formatJoinedAgo(entity.getJoinedAt()));

            UserEntity user = userRepository.findById(entity.getUserId().intValue()).orElse(null);

            if (user != null) {
                dto.setNickname(user.getUserNickname());
                dto.setUserId(user.getUserId());

                if (user.getPuuid() != null && !user.getPuuid().isEmpty()) {
                    matchOverallSummaryRepository.findByPuuid(user.getPuuid()).ifPresentOrElse(summary -> {
                        try {
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

                            RiotSummonerResponse response = riotApiService.getSummonerByPuuid(user.getPuuid());
                            if (response != null) {
                                String profileIconUrl = imageService.getProfileIconUrl(response.getProfileIconId());
                                dto.setProfileIconUrl(profileIconUrl);
                            }
                        } catch (Exception e) {
                            System.err.println("요약 정보 처리 중 오류: " + e.getMessage());
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
            }

            dto.setIntro(entity.getIntro() != null ? entity.getIntro() : "소개글이 없습니다.");

            if (dto.getProfileIconUrl() == null || dto.getProfileIconUrl().isBlank()) {
                dto.setProfileIconUrl("/images/clan/clan_default.png");
            }

        } catch (Exception e) {
            throw new RuntimeException("ClanMemberEntity → DTO 변환 중 오류 발생", e);
        }

        return dto;
    }

    @Override
    public Optional<ClanMemberDTO> findByClanIdAndUserId(Long clanId, Long userId) {
        return clanMemberRepository.findByClanIdAndUserId(clanId, userId)
                .flatMap(entity -> {
                    try {
                        return Optional.of(convertToDTO(entity));
                    } catch (Exception e) {
                        System.err.println("ClanMemberEntity → DTO 변환 중 오류 발생: " + e.getMessage());
                        return Optional.empty(); // 변환 실패 시 빈 Optional 반환
                    }
                });
    }

    @Override
    @Transactional
    public void updateMemberInfo(Long userId, Long clanId, ClanMemberDTO dto) {
        try {
            ClanMemberEntity entity = clanMemberRepository.findByClanIdAndUserId(clanId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("해당 멤버를 찾을 수 없습니다."));

            entity.setPosition(dto.getPosition());
            entity.setIntro(dto.getIntro());
        } catch (IllegalArgumentException e) {
            System.err.println("[updateMemberInfo] 잘못된 접근: " + e.getMessage());
            throw e; // 그대로 위로 던져서 Controller에서 처리
        } catch (Exception e) {
            System.err.println("[updateMemberInfo] 알 수 없는 오류 발생: " + e.getMessage());
            throw new RuntimeException("멤버 정보 수정 중 오류가 발생했습니다.");
        }
    }

    @Override
    @Transactional
    public void addMember(Long clanId, Long userId, String position, String tier, String intro) {
        Position posEnum;

        try {
            posEnum = Position.valueOf(position);
        } catch (IllegalArgumentException e) {
            System.err.println("[addMember] 잘못된 포지션 값: " + position);
            throw new IllegalArgumentException("잘못된 포지션 값입니다: " + position);
        }

        try {
            ClanMemberEntity entity = ClanMemberEntity.builder()
                    .clanId(clanId)
                    .userId(userId)
                    .position(posEnum)
                    .tier(tier)
                    .intro(intro)
                    .status("APPROVED")
                    .role("MEMBER")
                    .build();

            clanMemberRepository.save(entity);
        } catch (Exception e) {
            System.err.println("[addMember] 클랜 멤버 저장 실패: " + e.getMessage());
            throw new RuntimeException("클랜 멤버 등록 중 오류가 발생했습니다.");
        }
    }

    @Override
    @Transactional
    public void delegateLeader(Long clanId, Long fromUserSeq, Long toUserSeq) {
        if (fromUserSeq.equals(toUserSeq)) {
            throw new IllegalArgumentException("본인에게는 리더를 위임할 수 없습니다.");
        }

        // 현재 리더 확인
        ClanMemberEntity leader = clanMemberRepository.findByClanIdAndUserId(clanId, fromUserSeq)
                .orElseThrow(() -> new IllegalStateException("현재 리더 정보를 찾을 수 없습니다."));

        if (!"LEADER".equals(leader.getRole())) {
            throw new IllegalStateException("리더만 권한을 위임할 수 있습니다.");
        }

        // 위임 대상 확인
        ClanMemberEntity target = clanMemberRepository.findByClanIdAndUserId(clanId, toUserSeq)
                .orElseThrow(() -> new IllegalArgumentException("위임 대상 멤버가 존재하지 않습니다."));

        // 클랜 엔티티 확인
        ClanEntity clan = clanRepository.findById(clanId)
                .orElseThrow(() -> new IllegalStateException("클랜 정보를 찾을 수 없습니다."));

        // 역할 변경
        leader.setRole("MEMBER");
        target.setRole("LEADER");
        clan.setLeaderId(toUserSeq);
    }

    @Override
    @Transactional
    public void kickMember(Long clanId, Long requesterUserSeq, Long targetUserSeq) {
        // 클랜 존재 여부 확인
        ClanEntity clan = clanRepository.findById(clanId)
                .orElseThrow(() -> new IllegalArgumentException("해당 클랜이 존재하지 않습니다."));

        // 자기 자신 추방 방지
        if (requesterUserSeq.equals(targetUserSeq)) {
            throw new IllegalArgumentException("자기 자신은 추방할 수 없습니다.");
        }

        // 리더 권한 확인
        if (!clan.getLeaderId().equals(requesterUserSeq)) {
            throw new IllegalStateException("리더만 멤버를 추방할 수 있습니다.");
        }

        // 대상 멤버 확인
        ClanMemberEntity targetMember = clanMemberRepository.findByClanIdAndUserId(clanId, targetUserSeq)
                .orElseThrow(() -> new IllegalArgumentException("추방 대상 멤버를 찾을 수 없습니다."));

        // 삭제
        clanMemberRepository.delete(targetMember);
    }

    @Override
    public void leaveClan(Long clanId, Long userSeq) {

        ClanMemberEntity member = clanMemberRepository
                .findByClanIdAndUserId(clanId, userSeq)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저는 클랜 멤버가 아닙니다."));

        ClanEntity clan = clanRepository.findById(clanId)
                .orElseThrow(() -> new IllegalArgumentException("클랜을 찾을 수 없습니다."));

        if (clan.getLeaderId().equals(userSeq)) {
            throw new IllegalStateException("클랜 리더는 탈퇴할 수 없습니다. 리더 위임 후 탈퇴하세요.");
        }

        clanMemberRepository.delete(member);
    }

    @Override
    public int countPendingMembers(Long clanId) {
        if (!clanRepository.existsById(clanId)) {
            throw new IllegalArgumentException("존재하지 않는 클랜입니다.");
        }

        return clanMemberRepository.countByClanIdAndStatus(clanId, "PENDING");
    }

    private String formatJoinedAgo(LocalDateTime joinedAt) {
        if (joinedAt == null) return "";

        Duration duration = Duration.between(joinedAt, LocalDateTime.now());

        long days = duration.toDays();
        if (days > 0) return days + "일 전";

        long hours = duration.toHours() % 24;
        if (hours > 0) return hours + "시간 전";

        long minutes = duration.toMinutes() % 60;
        if (minutes > 0) return minutes + "분 전";

        return "방금 전";
    }
}