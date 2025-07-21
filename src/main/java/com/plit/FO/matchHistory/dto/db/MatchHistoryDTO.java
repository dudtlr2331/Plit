package com.plit.FO.matchHistory.dto.db;

import com.plit.FO.matchHistory.entity.MatchSummaryEntity;
import com.plit.FO.matchHistory.service.ImageService;
import com.plit.FO.matchHistory.service.MatchHelper;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchHistoryDTO { // 최근 전적 리스트 한 줄씩 요약

    private String puuid;
    private String position;

    private String matchId;
    private String championName;
    private int championLevel;
    private String tier;
    private String teamPosition;

    private String queueType; // 큐타입
    private String gameMode;
    private boolean win;

    private int kills;
    private double killParticipation; // 킬관여
    private int deaths;
    private int assists;
    private int cs;
    private double csPerMin;
    private double kdaRatio;
    private int damageDealt;
    private int damageTaken;
    private List<MatchPlayerDTO> matchPlayers;

    private int gameDurationSeconds;
    private LocalDateTime gameEndTimestamp; // 사용자에게 보여주는 용도라
    private String timeAgo;

    private String championImageUrl;
    private String profileIconUrl;
    private List<String> itemImageUrls;

    private String mainRune1Url;
    private String mainRune2Url;

    private String spell1ImageUrl;
    private String spell2ImageUrl;

    private String tierImageUrl;

    private List<String> traitIds;
    private List<String> traitImageUrls;

    private List<String> otherSummonerNames;

    private List<String> otherProfileIconUrls;


    // 걸린 시간 (분)
    public int getGameDurationMinutes() {
        return gameDurationSeconds / 60;
    }

    // 남은 시간 (초)
    public int getGameDurationRemainSeconds() {
        return gameDurationSeconds % 60;
    }

    public static MatchHistoryDTO fromEntities(
            MatchSummaryEntity summary,
            List<MatchPlayerDTO> players,
            ImageService imageService
    ) {
        int cs = summary.getCs();
        int duration = summary.getGameDurationSeconds();
        double csPerMin = (duration > 0) ? ((double) cs / duration) * 60 : 0;
        double kdaRatio = MatchHelper.getKda(summary.getKills(), summary.getDeaths(), summary.getAssists());

        return MatchHistoryDTO.builder()
                .matchId(summary.getMatchId())
                .puuid(summary.getPuuid())
                .gameEndTimestamp(summary.getGameEndTimestamp())
                .gameMode(summary.getGameMode())
                .queueType(summary.getQueueType())
                .win(summary.isWin())

                .championName(summary.getChampionName())
                .championLevel(summary.getChampionLevel())
                .championImageUrl(imageService.getImageUrl(summary.getChampionName(), "champion"))

                .tier(summary.getTier())
                .tierImageUrl(imageService.getImageUrl(summary.getTier(), "tier"))

                .teamPosition(summary.getTeamPosition())
                .position(summary.getTeamPosition())

                .kills(summary.getKills())
                .deaths(summary.getDeaths())
                .assists(summary.getAssists())
                .kdaRatio(kdaRatio)

                .cs(cs)
                .csPerMin(csPerMin)
                .damageDealt(summary.getDamageDealt())
                .damageTaken(summary.getDamageTaken())
                .killParticipation(summary.getKillParticipation())

                .gameDurationSeconds(duration)

                .spell1ImageUrl(imageService.getImageUrl(summary.getSpell1Id() + ".png", "spell"))
                .spell2ImageUrl(imageService.getImageUrl(summary.getSpell2Id() + ".png", "spell"))

                .mainRune1Url(imageService.getImageUrl(summary.getMainRune1Id() + ".png", "rune"))
                .mainRune2Url(imageService.getImageUrl(summary.getMainRune2Id() + ".png", "rune"))

                .itemImageUrls(imageService.getItemImageUrls(summary.getItemIds())) // List<String>

                .traitIds(MatchHelper.splitString(summary.getTraitIds()))
                .traitImageUrls(imageService.getTraitImageUrls(MatchHelper.splitString(summary.getTraitIds())))

                .otherSummonerNames(MatchHelper.splitString(summary.getOtherSummonerNames()))
                .otherProfileIconUrls(imageService.getProfileIconUrls(MatchHelper.splitString(summary.getOtherProfileIconIds())))

                .matchPlayers(players)
                .build();
    }



}
