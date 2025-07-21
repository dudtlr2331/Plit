package com.plit.FO.matchHistory.dto.db;

import com.plit.FO.matchHistory.dto.riot.RiotParticipantDTO;
import com.plit.FO.matchHistory.entity.MatchPlayerEntity;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.plit.FO.matchHistory.service.MatchHelper.round;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchPlayerDTO { // 매치 각각 상세정보 -> 소환사 1명의 정보

    private String puuid;
    private String matchId; // 하나의 게임 고유 ID
    private boolean win;
    private int teamId;
    private String teamPosition;
    private String tier;

    // 기본 정보
    private String championName;
    private String championKorName;
    private String summonerName;
    private int profileIconId;
    private String profileIconUrl;

    // 전투 정보
    private int kills;
    private int deaths;
    private int assists;

    private Integer damageDealt;
    private int totalDamageDealtToChampions;
    private int totalDamageTaken;

    private int cs;
    private double csPerMin;

    private String killParticipation;

    private double kdaRatio;

    private String badge;

    private int goldEarned;

    // 시간
    private LocalDateTime gameEndTimestamp;
    private int gameDurationMinutes;
    private int gameDurationSeconds;
    private String timeAgo;

    // 게임 종류
    private String gameMode;
    private String queueType;

    // 시야 - 와드
    private int wardsPlaced;
    private int wardsKilled;

    // 아이템
    private List<String> itemIds;
    private List<String> itemImageUrls;

    // 룬
    private int mainRune1;
    private int mainRune2;
    private int statRune1;
    private int statRune2;
    private int spell1Id;
    private int spell2Id;

    private String mainRune1Url;
    private String mainRune2Url;
    private String statRune1Url;
    private String statRune2Url;

    // 챔피언 이미지 경로
    private String championImageUrl;

    // 스펠 이미지 경로
    private String spell1ImageUrl;
    private String spell2ImageUrl;

    // 티어 이미지 경로
    private String tierImageUrl;

    public static MatchPlayerDTO fromRiotParticipant(RiotParticipantDTO p, int durationSec, LocalDateTime endTime, String gameMode, String queueType) {
        int cs = p.getTotalMinionsKilled() + p.getNeutralMinionsKilled();
        double csPerMin = durationSec > 0 ? ((double) cs) / (durationSec / 60.0) : 0.0;
        double kdaRatio = (double)(p.getKills() + p.getAssists()) / Math.max(p.getDeaths(), 1);

        return MatchPlayerDTO.builder()
                .puuid(p.getPuuid())
                .summonerName(p.getSummonerName())
                .championName(p.getChampionName())
                .kills(p.getKills())
                .deaths(p.getDeaths())
                .assists(p.getAssists())
                .goldEarned(p.getGoldEarned())
                .damageDealt(p.getTotalDamageDealtToChampions())
                .totalDamageDealtToChampions(p.getTotalDamageDealtToChampions())
                .totalDamageTaken(p.getTotalDamageTaken())
                .teamPosition(p.getTeamPosition())
                .win(p.isWin())
                .teamId(p.getTeamId())
                .itemIds(p.getItemIds().stream().map(String::valueOf).toList())
                .profileIconId(p.getProfileIconId())
                .cs(p.getTotalMinionsKilled() + p.getNeutralMinionsKilled())
                .csPerMin(csPerMin)
                .kdaRatio(round(kdaRatio,1))
                .gameEndTimestamp(endTime)
                .gameDurationSeconds(durationSec)
                .gameDurationMinutes(durationSec / 60)
                .gameMode(gameMode)
                .queueType(queueType)
                .wardsPlaced(p.getWardsPlaced())
                .wardsKilled(p.getWardsKilled())
                .mainRune1(p.getPerkPrimaryStyle())
                .mainRune2(p.getPerkSubStyle())
                .statRune1(p.getStatRune1())
                .statRune2(p.getStatRune2())
                .spell1Id(p.getSummoner1Id())
                .spell2Id(p.getSummoner2Id())
                .build();
    }

    public static List<MatchPlayerDTO> fromRiotParticipantList(List<RiotParticipantDTO> participants, String matchId,
                                                               int durationSec, LocalDateTime endTime,
                                                               String gameMode, String queueType, String tier) {
        return participants.stream()
                .map(p -> {
                    MatchPlayerDTO dto = MatchPlayerDTO.fromRiotParticipant(p, durationSec, endTime, gameMode, queueType);
                    dto.setMatchId(matchId);
                    dto.setTier(tier);

                    return dto;
                })
                .collect(Collectors.toList());
    }

    public static MatchPlayerDTO fromEntity(MatchPlayerEntity e) {
        return MatchPlayerDTO.builder()
                .puuid(e.getPuuid())
                .summonerName(e.getSummonerName())
                .championName(e.getChampionName())
                .kills(e.getKills())
                .deaths(e.getDeaths())
                .assists(e.getAssists())
                .goldEarned(e.getGoldEarned())
                .damageDealt(e.getDamageDealt())
                .totalDamageDealtToChampions(e.getTotalDamageDealtToChampions())
                .totalDamageTaken(e.getTotalDamageTaken())
                .teamPosition(e.getTeamPosition())
                .win(e.isWin())
                .teamId(e.getTeamId())
                .itemIds(Arrays.asList(e.getItemIds().split(",")))
                .profileIconId(e.getProfileIconId())
                .cs(e.getCs())
                .csPerMin(e.getCsPerMin())
                .mainRune1(e.getMainRune1())
                .mainRune2(e.getMainRune2())
                .spell1Id(e.getSpell1Id())
                .spell2Id(e.getSpell2Id())
                .build();
    }

}
