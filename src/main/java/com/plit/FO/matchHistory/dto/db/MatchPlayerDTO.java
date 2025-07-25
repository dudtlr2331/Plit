package com.plit.FO.matchHistory.dto.db;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plit.FO.matchHistory.dto.riot.RiotParticipantDTO;
import com.plit.FO.matchHistory.entity.MatchPlayerEntity;
import com.plit.FO.matchHistory.service.ImageService;
import com.plit.FO.matchHistory.service.MatchHelper;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.plit.FO.matchHistory.service.MatchHelper.round;

@Slf4j
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
    private String gameName;
    private String tagLine;

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
    private List<String> traitIds;

    private String mainRune1Url;
    private String mainRune2Url;
    private String statRune1Url;
    private String statRune2Url;

    // 챔피언 이미지 경로
    private String championImageUrl;

    // 스펠 이미지 경로
    private String spell1ImageUrl;
    private String spell2ImageUrl;

    private List<String> traitImageUrls;

    // 티어 이미지 경로
    private String tierImageUrl;

    public static MatchPlayerDTO fromRiotParticipant(RiotParticipantDTO p, int durationSec, LocalDateTime endTime, String gameMode, String queueType) {
        int cs = p.getTotalMinionsKilled() + p.getNeutralMinionsKilled();
        double csPerMin = durationSec > 0 ? ((double) cs) / (durationSec / 60.0) : 0.0;
        double kdaRatio = (double)(p.getKills() + p.getAssists()) / Math.max(p.getDeaths(), 1);

        int mainRune1 = 0;
        int mainRune2 = 0;

        try {
            if (p.getStyles() != null && !p.getStyles().isEmpty()) {
                RiotParticipantDTO.Style primary = p.getStyles().get(0);
                if (primary.getSelections() != null && !primary.getSelections().isEmpty()) {
                    mainRune1 = primary.getSelections().get(0).getPerk();
                }

                if (p.getStyles().size() > 1) {
                    RiotParticipantDTO.Style subStyle = p.getStyles().get(1);
                    mainRune2 = subStyle.getStyle();
                }
            }
        } catch (Exception e) {
            log.warn("룬 파싱 실패: {}", p.getPuuid(), e);
        }


        // traitIds 추출 (Arena 모드일 경우)
        List<String> traitIds = null;
        try {
            if ("CHERRY".equalsIgnoreCase(gameMode) && p.getTraits() != null) {
                traitIds = p.getTraits().stream()
                        .map(trait -> String.valueOf(trait.get("id")))
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("traitIds 추출 실패: {}", p.getPuuid(), e);
        }

        log.info("mainRune1: {}, mainRune2: {}", mainRune1, mainRune2);


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
                .csPerMin(round(csPerMin,1))
                .kdaRatio(round(kdaRatio,1))
                .gameEndTimestamp(endTime)
                .gameDurationSeconds(durationSec)
                .gameDurationMinutes(durationSec / 60)
                .gameMode(gameMode)
                .queueType(queueType)
                .wardsPlaced(p.getWardsPlaced())
                .wardsKilled(p.getWardsKilled())
                .mainRune1(mainRune1)
                .mainRune2(mainRune2)
                .statRune1(p.getStatRune1())
                .statRune2(p.getStatRune2())
                .spell1Id(p.getSummoner1Id())
                .spell2Id(p.getSummoner2Id())
                .traitIds(traitIds)
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

                    dto.setChampionImageUrl(MatchHelper.getImageUrl(dto.getChampionName() + ".png", "champion"));

                    dto.setMainRune1Url(MatchHelper.getImageUrl(dto.getMainRune1() + ".png", "rune"));
                    dto.setMainRune2Url(MatchHelper.getImageUrl(dto.getMainRune2() + ".png", "rune"));
                    dto.setStatRune1Url(MatchHelper.getImageUrl(dto.getStatRune1() + ".png", "rune"));
                    dto.setStatRune2Url(MatchHelper.getImageUrl(dto.getStatRune2() + ".png", "rune"));

                    dto.setSpell1ImageUrl(MatchHelper.getImageUrl(String.valueOf(dto.getSpell1Id()), "spell"));
                    dto.setSpell2ImageUrl(MatchHelper.getImageUrl(String.valueOf(dto.getSpell2Id()), "spell"));

                    dto.setStatRune1Url(MatchHelper.getImageUrl(dto.getStatRune1() + ".png", "rune"));
                    dto.setStatRune2Url(MatchHelper.getImageUrl(dto.getStatRune2() + ".png", "rune"));

                    dto.setTierImageUrl(dto.getTier() != null
                            ? MatchHelper.getImageUrl(dto.getTier().toUpperCase() + ".png", "tier")
                            : "/images/riot_default.png");

                    dto.setItemImageUrls(
                            dto.getItemIds().stream()
                                    .map(MatchHelper::getItemImageUrl)
                                    .collect(Collectors.toList())
                    );


                    return dto;
                })
                .collect(Collectors.toList());
    }

    public static MatchPlayerDTO fromEntity(MatchPlayerEntity e) {

        MatchPlayerDTO dto = MatchPlayerDTO.builder()
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
                .csPerMin(round(e.getCsPerMin(),1))
                .mainRune1(e.getMainRune1())
                .mainRune2(e.getMainRune2())
                .spell1Id(e.getSpell1Id())
                .spell2Id(e.getSpell2Id())
                .build();

        dto.setItemImageUrls(
                dto.getItemIds().stream()
                        .map(MatchHelper::getItemImageUrl)
                        .collect(Collectors.toList())
        );

        dto.setChampionImageUrl(MatchHelper.getImageUrl(dto.getChampionName() + ".png", "champion"));

        if (dto.getTier() != null) {
            dto.setTierImageUrl(MatchHelper.getImageUrl(dto.getTier().toUpperCase() + ".png", "tier"));
        } else {dto.setTierImageUrl("/images/riot_default.png");
        }

        dto.setGameName(e.getGameName());
        dto.setTagLine(e.getTagLine());

        if ("CHERRY".equalsIgnoreCase(e.getGameMode()) && e.getTraitIds() != null) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                List<Integer> traitIds = objectMapper.readValue(e.getTraitIds(), new TypeReference<>() {
                });
                dto.setTraitIds(traitIds.stream().map(String::valueOf).toList());

                List<String> traitImageUrls = traitIds.stream()
                        .map(id -> "/images/trait/" + id + ".png")
                        .collect(Collectors.toList());

                dto.setTraitImageUrls(traitImageUrls);
            } catch (Exception ex) {
                dto.setTraitImageUrls(List.of());
            }
        }
        int fixedMainRune1 = MatchHelper.PERK_TO_STYLE_MAP.getOrDefault(dto.getMainRune1(), dto.getMainRune1());
        int fixedMainRune2 = MatchHelper.PERK_TO_STYLE_MAP.getOrDefault(dto.getMainRune2(), dto.getMainRune2());

        dto.setMainRune1Url(MatchHelper.getImageUrl(fixedMainRune1 + ".png", "rune"));
        dto.setMainRune2Url(MatchHelper.getImageUrl(fixedMainRune2 + ".png", "rune"));

        dto.setSpell1ImageUrl(MatchHelper.getImageUrl(String.valueOf(dto.getSpell1Id()), "spell"));
        dto.setSpell2ImageUrl(MatchHelper.getImageUrl(String.valueOf(dto.getSpell2Id()), "spell"));

        dto.setStatRune1Url(MatchHelper.getImageUrl(dto.getStatRune1() + ".png", "rune"));
        dto.setStatRune2Url(MatchHelper.getImageUrl(dto.getStatRune2() + ".png", "rune"));

        return dto;
    }

    public String getKdaString() {
        return String.format("%d / %d / %d (%.1f)", kills, deaths, assists, kdaRatio);
    }


}
