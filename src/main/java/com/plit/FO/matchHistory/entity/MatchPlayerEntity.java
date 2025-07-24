package com.plit.FO.matchHistory.entity;

import com.plit.FO.matchHistory.dto.db.MatchDetailDTO;
import com.plit.FO.matchHistory.dto.db.MatchPlayerDTO;
import com.plit.FO.matchHistory.dto.riot.RiotParticipantDTO;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import static com.plit.FO.matchHistory.service.MatchHelper.round;

@Entity
@Table(name = "match_player")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchPlayerEntity { // 매치 상세페이지 데이터 저장

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String puuid;

    @Column(name = "match_id")
    private String matchId;
    private String summonerName;
    private String championName;

    @Column
    private String gameName;

    @Column
    private String tagLine;

    private int kills;
    private int deaths;
    private int assists;
    private double kdaRatio;

    private int cs; // 미니언 처치수
    private double csPerMin;
    private Integer damageDealt;
    private int totalDamageDealtToChampions;
    private int totalDamageTaken;

    private String teamPosition;

    private int mainRune1;
    private int mainRune2;
    private int statRune1;
    private int statRune2;
    @Column(name = "spell1_id")
    private int spell1Id;
    @Column(name = "spell2_id")
    private int spell2Id;

    private int wardsPlaced;
    private int wardsKilled;

    private LocalDateTime gameEndTimestamp;
    private int gameDurationMinutes;
    private int gameDurationSeconds;

    private String gameMode;
    private String queueType;

    private int teamId;
    private boolean win;

    private int goldEarned;

    // 문자열로 콤마(,) 구분하여 저장
    private String itemIds;

    @Column(name = "profile_icon_id")
    private Integer profileIconId;

    // dto -> entity ( db 저장 전에 가공 )
    public static MatchPlayerEntity fromDTO(MatchPlayerDTO dto) {
        System.out.println("Entity 저장 전: matchId = " + dto.getMatchId());
        return MatchPlayerEntity.builder()
                .matchId(dto.getMatchId())
                .puuid(dto.getPuuid())
                .profileIconId(dto.getProfileIconId())
                .summonerName(dto.getSummonerName())
                .championName(dto.getChampionName())
                .gameName(dto.getGameName())
                .tagLine(dto.getTagLine())
                .kills(dto.getKills())
                .deaths(dto.getDeaths())
                .assists(dto.getAssists())
                .kdaRatio(round(dto.getKdaRatio(),1))
                .cs(dto.getCs())
                .csPerMin(round(dto.getCsPerMin(),1))
                .damageDealt(dto.getDamageDealt())
                .totalDamageDealtToChampions(dto.getTotalDamageDealtToChampions())
                .totalDamageTaken(dto.getTotalDamageTaken())
                .teamPosition(dto.getTeamPosition())
                .mainRune1(dto.getMainRune1())
                .mainRune2(dto.getMainRune2())
                .statRune1(dto.getStatRune1())
                .statRune2(dto.getStatRune2())
                .spell1Id(dto.getSpell1Id())
                .spell2Id(dto.getSpell2Id())
                .wardsPlaced(dto.getWardsPlaced())
                .wardsKilled(dto.getWardsKilled())
                .gameEndTimestamp(dto.getGameEndTimestamp())
                .gameDurationMinutes(dto.getGameDurationMinutes())
                .gameDurationSeconds(dto.getGameDurationSeconds())
                .gameMode(dto.getGameMode())
                .queueType(dto.getQueueType())
                .teamId(dto.getTeamId())
                .win(dto.isWin())
                .goldEarned(dto.getGoldEarned())
                .itemIds(String.join(",", dto.getItemIds()))
                .build();

    }

}
