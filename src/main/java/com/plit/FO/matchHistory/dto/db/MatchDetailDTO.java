package com.plit.FO.matchHistory.dto.db;

import com.plit.FO.matchHistory.dto.MatchObjectiveDTO;
import com.plit.FO.matchHistory.dto.riot.RiotMatchInfoDTO;
import com.plit.FO.matchHistory.dto.riot.RiotParticipantDTO;
import com.plit.FO.matchHistory.entity.MatchPlayerEntity;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.plit.FO.matchHistory.service.MatchHelper.round;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchDetailDTO { // 최근 매치 상세 정보 - 팀으로

    private String myPuuid;
    private List<RiotParticipantDTO> participants;

    private String gameMode;
    private String queueType;
    private LocalDateTime gameEndTimestamp;
    private String matchId;

    private int totalMaxDamage;

    private List<MatchPlayerDTO> blueTeam;
    private List<MatchPlayerDTO> redTeam;
    private List<String> otherSummonerNames;

    // 바로 출력 가능하게 가공
    private MatchObjectiveDTO blueObjectives;
    private MatchObjectiveDTO redObjectives;

    private boolean blueWin;

    private List<Integer> otherProfileIconIds;

    private int gameDurationSeconds;

    // 매치 참여자들 중 입힌 데미지 딜량 최대값
    private int calculateMaxDamage() {
        if (participants == null || participants.isEmpty()) return 0;

        return participants.stream()
                .mapToInt(RiotParticipantDTO::getTotalDamageDealtToChampions)
                .max()
                .orElse(0);
    }

    // DB 저장용 entity 목록
    public List<MatchPlayerEntity> toPlayerEntities() {
        if (participants == null || participants.isEmpty()) return new ArrayList<>();

        return participants.stream()
                .map(p -> {
                    int cs = p.getTotalMinionsKilled() + p.getNeutralMinionsKilled();
                    double csPerMin = gameDurationSeconds > 0 ? (double) cs / (gameDurationSeconds / 60.0) : 0;

                    double kdaRatio = (p.getDeaths() == 0) ? (p.getKills() + p.getAssists()) : ((double)(p.getKills() + p.getAssists()) / p.getDeaths());

                    return MatchPlayerEntity.builder()
                            .matchId(matchId)
                            .puuid(p.getPuuid() != null ? p.getPuuid() : myPuuid)
                            .summonerName(p.getSummonerName())
                            .championName(p.getChampionName())
                            .teamPosition(p.getTeamPosition())
                            .kills(p.getKills())
                            .deaths(p.getDeaths())
                            .assists(p.getAssists())
                            .win(p.isWin())
                            .cs(cs)
                            .csPerMin(csPerMin)
                            .kdaRatio(round(kdaRatio,1))

                            .mainRune1(p.getPerkPrimaryStyle())
                            .mainRune2(p.getPerkSubStyle())
                            .statRune1(p.getStatRune1())
                            .statRune2(p.getStatRune2())

                            .profileIconId(p.getProfileIconId())

                            .itemIds(convertItemIds(p.getItemIds()))

                            .gameMode(gameMode)
                            .queueType(queueType)

                            .gameEndTimestamp(gameEndTimestamp)
                            .gameDurationSeconds(gameDurationSeconds)
                            .gameDurationMinutes(gameDurationSeconds / 60)

                            .build();
                })
                .collect(Collectors.toList());
    }

    // 아이템 아이디 콤마 구분하여 String 으로
    private String convertItemIds(List<Integer> itemIds) {
        if (itemIds == null) return "";
        return itemIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    // 주요 필드
    public MatchDetailDTO(RiotMatchInfoDTO matchInfo, String matchId, String puuid) {
        this.matchId = matchId;
        this.gameMode = matchInfo.getGameMode();
        this.gameEndTimestamp = Instant.ofEpochMilli(matchInfo.getGameEndTimestamp())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        this.participants = matchInfo.getParticipants();
        this.gameDurationSeconds = matchInfo.getGameDurationSeconds();

        // 초기값 세팅
        this.totalMaxDamage = calculateMaxDamage();
        this.blueTeam = new ArrayList<>();
        this.redTeam = new ArrayList<>();
        this.blueObjectives = new MatchObjectiveDTO();
        this.redObjectives = new MatchObjectiveDTO();

        // riot api 기준 팀ID : 100 - 블루팀 / 200 - 레드팀
        // 승리 여부: 팀 ID 100( 블루팀 )이 승리한 경우 기준
        this.blueWin = participants.stream()
                .filter(p -> p.getTeamId() == 100)
                .findFirst()
                .map(RiotParticipantDTO::isWin)
                .orElse(true);  // 기본값 true

        this.otherSummonerNames = new ArrayList<>();
        this.otherProfileIconIds = new ArrayList<>();
        for (RiotParticipantDTO p : participants) {
            if (!puuid.equals(p.getPuuid())) {
                this.otherSummonerNames.add(p.getSummonerName());
                this.otherProfileIconIds.add(p.getProfileIconId());
            }
        }
    }


}