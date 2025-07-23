package com.plit.FO.matchHistory.dto.db;

import com.plit.FO.matchHistory.entity.MatchSummaryEntity;
import com.plit.FO.matchHistory.service.ImageService;
import com.plit.FO.matchHistory.service.MatchHelper;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

import static com.plit.FO.matchHistory.service.MatchHelper.round;

@Slf4j
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

    // html 에서 사용
    public int getGameDurationMinutes() {
        return gameDurationSeconds / 60;
    }

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

        MatchPlayerDTO self = players.stream()
                .filter(p -> p.getPuuid().equals(summary.getPuuid()))
                .findFirst()
                .orElse(null);

        if (self == null) {
            log.warn("No match player found for puuid: {}", summary.getPuuid());
            return null;
        }

        String mainRune1Url = imageService.getImageUrl(self.getMainRune1() + ".png", "rune");
        String mainRune2Url = imageService.getImageUrl(self.getMainRune2() + ".png", "rune");

        log.info("mainRune1: {}, url: {}", self.getMainRune1(), mainRune1Url);
        log.info("mainRune2: {}, url: {}", self.getMainRune2(), mainRune2Url);

        return MatchHistoryDTO.builder()
                .matchId(summary.getMatchId())
                .puuid(summary.getPuuid())
                .gameEndTimestamp(summary.getGameEndTimestamp())
                .gameMode(summary.getGameMode())
                .queueType(summary.getQueueType())
                .win(summary.isWin())

                .championName(summary.getChampionName())
                .championImageUrl(imageService.getImageUrl(summary.getChampionName() + ".png", "champion"))

                .tier(summary.getTier())
                .tierImageUrl(imageService.getImageUrl(summary.getTier() + ".png", "tier"))

                .teamPosition(summary.getTeamPosition())
                .position(summary.getTeamPosition())

                .kills(summary.getKills())
                .deaths(summary.getDeaths())
                .assists(summary.getAssists())
                .kdaRatio(round(kdaRatio,1))

                .cs(cs)
                .csPerMin(round(csPerMin, 1))
                .damageDealt(summary.getDamageDealt())
                .damageTaken(summary.getDamageTaken())
                .gameDurationSeconds(duration)

                .itemImageUrls(imageService.getItemImageUrls(summary.getItemIds()))
                .matchPlayers(players)

                .spell1ImageUrl(imageService.getImageUrl(String.valueOf(self.getSpell1Id()), "spell"))
                .spell2ImageUrl(imageService.getImageUrl(String.valueOf(self.getSpell2Id()), "spell"))

                .mainRune1Url(mainRune1Url)
                .mainRune2Url(mainRune2Url)

                .profileIconUrl(imageService.getImageUrl(self.getProfileIconId() + ".png", "profile-icon"))

                .traitIds(self.getTraitIds())
                .traitImageUrls(imageService.getTraitImageUrls(self.getTraitIds()))

                .timeAgo(MatchHelper.getTimeAgo(summary.getGameEndTimestamp()))

                .build();
    }



}
