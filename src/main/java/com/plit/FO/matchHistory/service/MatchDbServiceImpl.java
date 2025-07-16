package com.plit.FO.matchHistory.service;

import com.plit.FO.matchHistory.dto.*;
import com.plit.FO.matchHistory.dto.db.MatchDetailDTO;
import com.plit.FO.matchHistory.dto.db.MatchHistoryDTO;
import com.plit.FO.matchHistory.dto.db.MatchPlayerDTO;
import com.plit.FO.matchHistory.dto.riot.RiotMatchInfoDTO;
import com.plit.FO.matchHistory.dto.riot.RiotParticipantDTO;
import com.plit.FO.matchHistory.entity.ImageEntity;
import com.plit.FO.matchHistory.entity.MatchPlayerEntity;
import com.plit.FO.matchHistory.entity.MatchSummaryEntity;
import com.plit.FO.matchHistory.repository.MatchPlayerRepository;
import com.plit.FO.matchHistory.repository.MatchSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

import static com.plit.FO.matchHistory.service.MatchHelper.*;

@Service
@RequiredArgsConstructor
public class MatchDbServiceImpl implements MatchDbService{ // 전적 검색 DB 저장, 조회

    private final MatchSummaryRepository matchSummaryRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final RiotApiService riotApiService;
    private final ImageService imageService;

    private final RestTemplate restTemplate;

    @Value("${riot.api.key}")
    private String riotApiKey;



    // puuid -> 최근 match ID 조회 [ match/v5 ]
    public List<String> getRecentMatchIds(String puuid, int count) {
        try {
            String url = "https://asia.api.riotgames.com/lol/match/v5/matches/by-puuid/"
                    + puuid + "/ids?start=0&count=" + count + "&api_key=" + riotApiKey;

            String[] matchIds = restTemplate.getForObject(url, String[].class);
            return Arrays.asList(matchIds);
        } catch (Exception e) {
            System.err.println("매치 ID 조회 실패: " + e.getMessage());
            return List.of();
        }
    }

    private LocalDateTime toLocalDateTime(long epochMillis) {
        return Instant.ofEpochMilli(epochMillis)
                .atZone(ZoneId.of("Asia/Seoul"))
                .toLocalDateTime();
    }

    public void initMatchHistory(String puuid) {
        // 최근 match ID 20개 가져오기
        List<String> matchIds = getRecentMatchIds(puuid, 20);

        for (String matchId : matchIds) {
            // 이미 저장된 matchId는 건너뛰기
            if (matchSummaryRepository.existsByMatchId(matchId)) {
                continue;
            }

            // Riot API로 match 상세 정보 가져오기
            RiotMatchInfoDTO info = riotApiService.getMatchInfo(matchId);
            List<RiotParticipantDTO> participants = info.getParticipants();

            // 본인 participant만 추출
            RiotParticipantDTO me = participants.stream()
                    .filter(p -> puuid.equals(p.getPuuid()))
                    .findFirst()
                    .orElse(null);
            if (me == null) continue;

            LocalDateTime endTime = LocalDateTime.ofEpochSecond(info.getGameEndTimestamp() / 1000, 0, ZoneOffset.UTC);

            // 요약 정보 생성
            MatchSummaryEntity summary = MatchSummaryEntity.builder()
                    .matchId(matchId)
                    .puuid(puuid)
                    .win(me.isWin())
                    .teamPosition(me.getTeamPosition())
                    .championName(me.getChampionName())
                    .kills(me.getKills())
                    .deaths(me.getDeaths())
                    .assists(me.getAssists())
                    .kdaRatio(calculateKda(me.getKills(), me.getDeaths(), me.getAssists()))
                    .tier(riotApiService.getTierByPuuid(puuid))
                    .gameEndTimestamp(endTime)
                    .gameMode(info.getGameMode())
                    .champLevel(me.getChampLevel())
                    .cs(me.getTotalMinionsKilled())
                    .itemIds("")
                    .createdAt(null)
                    .build();

            String queueType = info.getQueueId();



            // 상세 정보 리스트 생성
            List<MatchPlayerEntity> players = participants.stream()
                    .map(p -> MatchPlayerEntity.builder()
                            .matchId(matchId)
                            .puuid(p.getPuuid())
                            .summonerName(p.getSummonerName())
                            .championName(p.getChampionName())
                            .kills(p.getKills())
                            .deaths(p.getDeaths())
                            .assists(p.getAssists())
                            .kdaRatio(calculateKda(p.getKills(), p.getDeaths(), p.getAssists()))
                            .cs(0) // CS는 없는 경우 0으로
                            .csPerMin(0)
                            .totalDamageDealtToChampions(p.getTotalDamageDealtToChampions())
                            .totalDamageTaken(p.getTotalDamageTaken())
                            .teamPosition(p.getTeamPosition())
                            .tier(riotApiService.getTierByPuuid(p.getPuuid()))
                            .mainRune1(0) // 추후 룬 파싱 가능하면 반영
                            .mainRune2(0)
                            .statRune1(0)
                            .statRune2(0)
                            .wardsPlaced(0)
                            .wardsKilled(0)
                            .gameEndTimestamp(toLocalDateTime(info.getGameEndTimestamp()))
                            .gameMode(info.getGameMode())
                            .queueType(queueType)
                            .teamId(p.getTeamId())
                            .win(p.isWin())
                            .itemIds("")
                            .goldEarned(p.getGoldEarned())
                            .build()
                    )
                    .collect(Collectors.toList());

            // 저장
            saveMatchHistory(summary, players);
        }
    }


    @Override
    public List<String> getMatchIdsByPuuid(String puuid) {
        return List.of();
    }

    // (*) puuid -> matchid <최근 매치 정보 - 전적 요약 리스트> [ match/v5 ]
    public List<MatchHistoryDTO> getMatchHistoryFromRiot(String puuid) {
        List<String> matchIds = getMatchIdsByPuuid(puuid);

        System.out.println("[getMatchHistory] puuid = " + puuid);
        System.out.println("[getMatchHistory] matchIds = " + matchIds);

        List<MatchHistoryDTO> result = new ArrayList<>();

        for (String matchId : matchIds) {
            try {
                String url = "https://asia.api.riotgames.com/lol/match/v5/matches/"
                        + matchId + "?api_key=" + riotApiKey;

                System.out.println("[getMatchIdsByPuuid] Request URL: " + url);

                ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
                Map<String, Object> matchData = response.getBody();

                Map<String, Object> metadata = (Map<String, Object>) matchData.get("metadata");
                Map<String, Object> info = (Map<String, Object>) matchData.get("info");

                List<Map<String, Object>> participants = (List<Map<String, Object>>) info.get("participants");

                for (Map<String, Object> p : participants) {
                    if (puuid.equals(p.get("puuid"))) {
                        int teamId = (int) p.get("teamId");
                        int durationSeconds = ((Number) info.get("gameDuration")).intValue();
                        int minutes = durationSeconds / 60;
                        int remainSeconds = durationSeconds % 60;


                        int teamTotalKills = participants.stream()
                                .filter(pp -> ((Number) pp.get("teamId")).intValue() == teamId)
                                .mapToInt(pp -> ((Number) pp.get("kills")).intValue())
                                .sum();

                        int kills = ((Number) p.get("kills")).intValue();
                        int assists = ((Number) p.get("assists")).intValue();
                        int deaths = ((Number) p.get("deaths")).intValue();
                        double kdaRatio = MatchHelper.calculateKda(kills, deaths, assists);

                        int totalMinions = ((Number) p.get("totalMinionsKilled")).intValue()
                                + ((Number) p.get("neutralMinionsKilled")).intValue();
                        double csPerMin = totalMinions / (durationSeconds / 60.0);

                        // 킬 관여율
                        double kp = MatchHelper.calculateKillParticipation(kills, assists, teamTotalKills);

                        List<String> itemImageUrls = new ArrayList<>();
                        List<String> itemIds = new ArrayList<>();

                        for (int i = 0; i <= 6; i++) {
                            int itemId = (int) p.get("item" + i);
                            itemIds.add(String.valueOf(itemId));
                            String itemUrl = itemId != 0
                                    ? imageService.getImage(String.valueOf(itemId) + ".png", "item")
                                    .map(ImageEntity::getImageUrl)
                                    .orElse("/images/default.png")
                                    : null;
                            itemImageUrls.add(itemUrl);
                        }

                        List<String> traitImageUrls = new ArrayList<>();
                        for (int i = 1; i <= 4; i++) {
                            Object raw = p.get("playerAugment" + i);
                            if (raw instanceof Integer) {
                                int augmentId = (int) raw;
                                String imageUrl = "/images/trait/" + augmentId + ".png";
                                traitImageUrls.add(imageUrl);
                            }
                        }

                        List<String> otherSummoners = participants.stream()
                                .map(participant -> (String) participant.get("summonerName"))
                                .collect(Collectors.toList());

                        List<String> metadataPuuidList = (List<String>) metadata.get("participants");


                        List<String> otherSummonerNames = new ArrayList<>();
                        List<String> otherProfileIconUrls = new ArrayList<>();

                        for (Map<String, Object> player : participants) {
                            String name = (String) player.get("summonerName");
                            int iconId = (int) player.get("profileIcon");

                            otherSummonerNames.add(name);

                            String iconUrl = imageService.getImage(iconId + ".png", "profile-icon")
                                    .map(ImageEntity::getImageUrl)
                                    .orElse("/images/default.png");

                            otherProfileIconUrls.add(iconUrl);
                        }

                        // imageService 에서 DB 에서 가져온 이미지 경로 매핑
                        String profileIconUrl = imageService.getImage(String.valueOf(p.get("profileIcon") + ".png"), "profile-icon")
                                .map(ImageEntity::getImageUrl)
                                .orElse("/images/default.png");

                        String championImageUrl = imageService.getImage((String) p.get("championName") + ".png", "champion")
                                .map(ImageEntity::getImageUrl)
                                .orElse("/images/default.png");

                        String spell1ImageUrl = imageService.getImage(p.get("summoner1Id") + ".png", "spell")
                                .map(ImageEntity::getImageUrl).orElse("/images/default.png");
                        String spell2ImageUrl = imageService.getImage(p.get("summoner2Id") + ".png", "spell")
                                .map(ImageEntity::getImageUrl).orElse("/images/default.png");

                        String mainRune1Url = imageService.getImage(p.get("perkPrimaryStyle") + ".png", "rune")
                                .map(ImageEntity::getImageUrl).orElse("/images/default.png");
                        String mainRune2Url = imageService.getImage(p.get("perkSubStyle") + ".png", "rune")
                                .map(ImageEntity::getImageUrl).orElse("/images/default.png");

                        String tier = riotApiService.getTierByPuuid(puuid);
                        String tierImageUrl = imageService.getImage(tier + ".png", "tier")
                                .map(ImageEntity::getImageUrl).orElse("/images/default.png");

                        LocalDateTime endTime = LocalDateTime.ofEpochSecond(
                                ((Number) info.get("gameEndTimestamp")).longValue() / 1000, 0, ZoneOffset.UTC);
                        String timeAgo = getTimeAgo(endTime);

                        MatchHistoryDTO dto = MatchHistoryDTO.builder()
                                .matchId(matchId)
                                .win((Boolean) p.get("win"))
                                .teamPosition((String) p.get("teamPosition"))
                                .championName((String) p.get("championName"))
                                .championLevel((int) p.get("champLevel"))
                                .kills(kills)
                                .deaths(deaths)
                                .assists(assists)
                                .kdaRatio(Math.round(kdaRatio * 100) / 100.0)
                                .cs(totalMinions)
                                .csPerMin(Math.round(csPerMin * 10) / 10.0)
                                .killParticipation(Math.round(kp * 10) / 10.0)
                                .gameMode((String) info.get("gameMode"))
                                .queueType(String.valueOf(info.get("queueId")))
                                .gameEndTimestamp(endTime)
                                .gameDurationSeconds(durationSeconds)
                                .timeAgo(timeAgo)
                                .championImageUrl(championImageUrl)
                                .profileIconUrl(profileIconUrl)
                                .itemImageUrls(itemImageUrls)
                                .spell1ImageUrl(spell1ImageUrl)
                                .spell2ImageUrl(spell2ImageUrl)
                                .mainRune1Url(mainRune1Url)
                                .mainRune2Url(mainRune2Url)
                                .tier(tier)
                                .tierImageUrl(tierImageUrl)
                                .traitImageUrls(traitImageUrls)
                                .otherSummonerNames(otherSummonerNames)
                                .otherProfileIconUrls(otherProfileIconUrls)
                                .build();


                        result.add(dto);
                        break;
                    }
                }

            } catch (Exception e) {
                System.err.println("매치 데이터 조회 실패 (" + matchId + "): " + e.getMessage());
            }
        }

        return result;
    }

    // matchId, puuid <상세 정보 필요할 때 한게임씩 가져오게!> [ match/v5 ]
    public MatchDetailDTO getMatchDetailFromRiot(String matchId, String myPuuid) {
        try {
            String url = "https://asia.api.riotgames.com/lol/match/v5/matches/" + matchId + "?api_key=" + riotApiKey;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<String, Object> matchData = response.getBody();
            Map<String, Object> info = (Map<String, Object>) matchData.get("info");
            List<Map<String, Object>> participants = (List<Map<String, Object>>) info.get("participants");
            List<Map<String, Object>> teams = (List<Map<String, Object>>) info.get("teams");

            int maxDamage = participants.stream()
                    .mapToInt(p -> (int) p.get("totalDamageDealtToChampions"))
                    .max().orElse(1);

            List<MatchPlayerDTO> blueTeam = new ArrayList<>();
            List<MatchPlayerDTO> redTeam = new ArrayList<>();

            Map<String,String> tierCache = new HashMap<>();

            int blueGold = 0;
            int redGold = 0;

            for (Map<String, Object> p : participants) {
                List<String> itemImageUrls = new ArrayList<>();
                for (int i = 0; i <= 6; i++) {
                    int itemId = (int) p.get("item" + i);
                    String itemUrl = itemId != 0
                            ? imageService.getImage(String.valueOf(itemId) + ".png", "item")
                            .map(ImageEntity::getImageUrl)
                            .orElse("")
                            : null;
                    itemImageUrls.add(itemUrl);
                }

                String puuid = (String) p.get("puuid");
                int kills = ((Number) p.get("kills")).intValue();
                int deaths = ((Number) p.get("deaths")).intValue();
                int assists = ((Number) p.get("assists")).intValue();

                double kdaRatio = deaths != 0 ? (double)(kills + assists) / deaths : kills + assists;

                String tier = tierCache.computeIfAbsent(puuid, k -> riotApiService.getTierByPuuid(k));
                String tierImageUrl = imageService.getImage(tier + ".png", "tier")
                        .map(ImageEntity::getImageUrl)
                        .orElse("");


                Map<String, Object> perks = (Map<String, Object>) p.get("perks");
                List<Map<String, Object>> styles = (List<Map<String, Object>>) perks.get("styles");

                int mainRune1 = (int) ((Map<String, Object>) ((List<?>) styles.get(0).get("selections")).get(0)).get("perk");
                int mainRune2 = (int) styles.get(1).get("style");

                Map<String, Object> statPerks = (Map<String, Object>) perks.get("statPerks");
                int statRune1 = (int) statPerks.get("offense");
                int statRune2 = (int) statPerks.get("flex");

                int cs = ((Number) p.get("totalMinionsKilled")).intValue()
                        + ((Number) p.get("neutralMinionsKilled")).intValue();
                double csPerMin = cs / (((Number) info.getOrDefault("gameDuration", 1)).doubleValue() / 60.0);


                String profileIconUrl = imageService.getImage(String.valueOf(p.get("profileIcon") + ".png"), "profile-icon")
                        .map(ImageEntity::getImageUrl)
                        .orElse("/images/default.png");

                String championImageUrl = imageService.getImage((String) p.get("championName") + ".png", "champion")
                        .map(ImageEntity::getImageUrl)
                        .orElse("/images/default.png");

                String mainRune1Url = imageService.getImage(String.valueOf(mainRune1) + ".png", "rune")
                        .map(ImageEntity::getImageUrl)
                        .orElse("/images/default.png");

                String mainRune2Url = imageService.getImage(String.valueOf(mainRune2) + ".png", "rune")
                        .map(ImageEntity::getImageUrl)
                        .orElse("/images/default.png");

                String statRune1Url = imageService.getImage(String.valueOf(statRune1) + ".png", "rune")
                        .map(ImageEntity::getImageUrl)
                        .orElse("/images/default.png");

                String statRune2Url = imageService.getImage(String.valueOf(statRune2) + ".png", "rune")
                        .map(ImageEntity::getImageUrl)
                        .orElse("/images/default.png");

                String teamPosition = normalizePosition((String) p.get("teamPosition"));

                MatchPlayerDTO dto = MatchPlayerDTO.builder()
                        .matchId(matchId)
                        .win((Boolean) p.get("win"))
                        .teamPosition(teamPosition)
                        .championName((String) p.get("championName"))
                        .kills(kills)
                        .deaths(deaths)
                        .assists(assists)
                        .summonerName((String) p.get("summonerName"))
                        .kdaRatio(Math.round(kdaRatio * 100) / 100.0)
                        .tier(tier)
                        .tierImageUrl(tierImageUrl)
                        .totalDamageDealtToChampions(((Number) p.get("totalDamageDealtToChampions")).intValue())
                        .totalDamageTaken(((Number) p.get("totalDamageTaken")).intValue())
                        .gameMode((String) info.get("gameMode"))
                        .gameEndTimestamp(LocalDateTime.ofEpochSecond(
                                ((Number) info.get("gameEndTimestamp")).longValue() / 1000, 0, ZoneOffset.UTC))
                        .profileIconUrl(profileIconUrl)
                        .championImageUrl(championImageUrl)
                        .mainRune1(mainRune1)
                        .mainRune2(mainRune2)
                        .statRune1(statRune1)
                        .statRune2(statRune2)
                        .mainRune1Url(mainRune1Url)
                        .mainRune2Url(mainRune2Url)
                        .statRune1Url(statRune1Url)
                        .statRune2Url(statRune2Url)
                        .itemImageUrls(itemImageUrls)
                        .cs(cs)
                        .csPerMin(csPerMin)
                        .wardsPlaced(((Number) p.getOrDefault("wardsPlaced", 0)).intValue())
                        .wardsKilled(((Number) p.getOrDefault("wardsKilled", 0)).intValue())
                        .build();

                int teamId = ((Number) p.get("teamId")).intValue();
                if (teamId == 100) {
                    blueTeam.add(dto);
                    blueGold += ((Number) p.get("goldEarned")).intValue();
                } else {
                    redTeam.add(dto);
                    redGold += ((Number) p.get("goldEarned")).intValue();
                }
            }

            List<Map<String, Object>> teamList = (List<Map<String, Object>>) info.get("teams");

            MatchObjectiveDTO blueObjectives = new MatchObjectiveDTO();
            MatchObjectiveDTO redObjectives = new MatchObjectiveDTO();
            boolean blueWin = false;

            for (Map<String, Object> team : teamList) {
                int teamId = ((Number) team.get("teamId")).intValue();
                Map<String, Object> objectives = (Map<String, Object>) team.get("objectives");

                int kills = ((Number) ((Map<String, Object>) objectives.get("champion")).get("kills")).intValue();
                int towers = ((Number) ((Map<String, Object>) objectives.get("tower")).get("kills")).intValue();
                int dragons = ((Number) ((Map<String, Object>) objectives.get("dragon")).get("kills")).intValue();
                int barons = ((Number) ((Map<String, Object>) objectives.get("baron")).get("kills")).intValue();
                int heralds = ((Number) ((Map<String, Object>) objectives.get("riftHerald")).get("kills")).intValue();

                if (teamId == 100) {
                    blueObjectives.setTotalKills(kills);
                    blueObjectives.setTowerKills(towers);
                    blueObjectives.setDragonKills(dragons);
                    blueObjectives.setBaronKills(barons);
                    blueObjectives.setHeraldKills(heralds);
                    blueObjectives.setRiftKills(0); // 필요시 따로 처리
                    blueObjectives.setTotalGold(blueGold);
                    blueWin = (Boolean) team.get("win");
                } else {
                    redObjectives.setTotalKills(kills);
                    redObjectives.setTowerKills(towers);
                    redObjectives.setDragonKills(dragons);
                    redObjectives.setBaronKills(barons);
                    redObjectives.setHeraldKills(heralds);
                    redObjectives.setRiftKills(0); // 필요시 따로 처리
                    redObjectives.setTotalGold(redGold);
                }
            }

            return MatchDetailDTO.builder()
                    .matchId(matchId)
                    .totalMaxDamage(maxDamage)
                    .blueTeam(blueTeam)
                    .redTeam(redTeam)
                    .blueObjectives(blueObjectives)
                    .redObjectives(redObjectives)
                    .blueWin(blueWin)
                    .build();

        } catch (Exception e) {
            System.err.println("상세 매치 조회 실패: " + e.getMessage());
            return null;
        }
    }



    // 요약 + 상세 정보 -> DB 저장
    public void saveMatchHistory(MatchSummaryEntity summary, List<MatchPlayerEntity> players) {
        // 중복 방지
        if (matchSummaryRepository.existsByMatchId(summary.getMatchId())) {
            return;
        }

        // 요약 저장
        matchSummaryRepository.save(summary);

        // 상세 저장
        for (MatchPlayerEntity player : players) {
            matchPlayerRepository.save(player);
        }
    }

    public void saveMatchHistory(String puuid) {
        List<String> matchIds = getMatchIdsByPuuid(puuid); // 20개

        for (String matchId : matchIds) {
            if (matchSummaryRepository.existsByMatchId(matchId)) continue;

            // Riot API로부터 MatchDetailDTO 받아오기
            MatchDetailDTO detail = getMatchDetailFromRiot(matchId, puuid);

            // DTO → Entity 변환
            MatchSummaryEntity summary = MatchSummaryEntity.fromDetailDTO(detail, puuid);
            List<MatchPlayerEntity> players = detail.toPlayerEntities();

            saveMatchHistory(summary, players);
        }
    }


    // 최신순으로 사용자의 요약 전적 20개 불러오기
    public List<MatchHistoryDTO> getMatchSummaryFromDB(String puuid) {
        List<MatchSummaryEntity> entities = matchSummaryRepository.findTop20ByPuuidOrderByGameEndTimestampDesc(puuid);

        return entities.stream()
                .map(entity -> MatchHistoryDTO.builder()
                        .matchId(entity.getMatchId())
                        .win(entity.isWin())
                        .teamPosition(entity.getTeamPosition())
                        .championName(entity.getChampionName())
                        .kills(entity.getKills())
                        .deaths(entity.getDeaths())
                        .assists(entity.getAssists())
                        .kdaRatio(entity.getKdaRatio())
                        .tier(entity.getTier())
                        .gameEndTimestamp(entity.getGameEndTimestamp())
                        .gameMode(entity.getGameMode())
                        .build())
                .collect(Collectors.toList());
    }

    // matchId 기준으로 모든 플레이어 정보 가져오기
    public MatchDetailDTO getMatchDetailFromDB(String matchId) {
        List<MatchPlayerEntity> players = matchPlayerRepository.findByMatchId(matchId);

        List<MatchPlayerDTO> blueTeam = new ArrayList<>();
        List<MatchPlayerDTO> redTeam = new ArrayList<>();

        int maxDamage = players.stream()
                .mapToInt(MatchPlayerEntity::getTotalDamageDealtToChampions)
                .max().orElse(1);

        int blueGold = 0;
        int redGold = 0;
        boolean blueWin = false;

        for (MatchPlayerEntity p : players) {
            MatchPlayerDTO dto = MatchPlayerDTO.builder()
                    .matchId(p.getMatchId())
                    .summonerName(p.getSummonerName())
                    .championName(p.getChampionName())
                    .kills(p.getKills())
                    .deaths(p.getDeaths())
                    .assists(p.getAssists())
                    .kdaRatio(p.getKdaRatio())
                    .cs(p.getCs())
                    .csPerMin(p.getCsPerMin())
                    .totalDamageDealtToChampions(p.getTotalDamageDealtToChampions())
                    .totalDamageTaken(p.getTotalDamageTaken())
                    .teamPosition(p.getTeamPosition())
                    .tier(p.getTier())
                    .mainRune1(p.getMainRune1())
                    .mainRune2(p.getMainRune2())
                    .statRune1(p.getStatRune1())
                    .statRune2(p.getStatRune2())
                    .itemIds(Arrays.asList(Optional.ofNullable(p.getItemIds()).orElse("").split(",")))
                    .wardsPlaced(p.getWardsPlaced())
                    .wardsKilled(p.getWardsKilled())
                    .gameEndTimestamp(p.getGameEndTimestamp())
                    .gameDurationMinutes(p.getGameDurationMinutes())
                    .gameDurationSeconds(p.getGameDurationSeconds())
                    .gameMode(p.getGameMode())
                    .queueType(p.getQueueType())
                    .build();

            if (p.getTeamId() == 100) {
                blueTeam.add(dto);
                blueGold += p.getGoldEarned();
                blueWin = p.isWin();
            } else {
                redTeam.add(dto);
                redGold += p.getGoldEarned();
            }
        }

        MatchObjectiveDTO blueObjectives = MatchObjectiveDTO.builder()
                .totalGold(blueGold)
                .build();

        MatchObjectiveDTO redObjectives = MatchObjectiveDTO.builder()
                .totalGold(redGold)
                .build();

        return MatchDetailDTO.builder()
                .matchId(matchId)
                .totalMaxDamage(maxDamage)
                .blueTeam(blueTeam)
                .redTeam(redTeam)
                .blueObjectives(blueObjectives)
                .redObjectives(redObjectives)
                .blueWin(blueWin)
                .build();
    }


    @Override
    public void updateMatchHistory(String puuid) {
        List<String> matchIds = getMatchIdsByPuuid(puuid);

        for (String matchId : matchIds) {
            if (matchSummaryRepository.existsByMatchId(matchId)) continue;

            // matchId 기반으로 Riot API로부터 상세 전적 받아오기
            MatchDetailDTO detail = getMatchDetailFromRiot(matchId, puuid);

            // 요약 정보로 변환하여 저장
            MatchSummaryEntity summary = MatchSummaryEntity.fromDetailDTO(detail, puuid);
            matchSummaryRepository.save(summary);

            // 참가자 정보 저장 // 내부 DTO( MatchPlayerDTO ) -> Entity 로 저장
            for (MatchPlayerDTO player : detail.getBlueTeam()) {
                matchPlayerRepository.save(MatchPlayerEntity.fromDTO(player));
            }
            for (MatchPlayerDTO player : detail.getRedTeam()) {
                matchPlayerRepository.save(MatchPlayerEntity.fromDTO(player));
            }
        }
    }

    @Override
    public List<MatchHistoryDTO> getRecentMatchHistories(String puuid) {
        return matchSummaryRepository.findTop20ByPuuidOrderByGameEndTimestampDesc(puuid)
                .stream()
                .map(MatchSummaryEntity::toDTO)
                .collect(Collectors.toList());
    }




}
