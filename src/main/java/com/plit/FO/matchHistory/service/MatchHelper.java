package com.plit.FO.matchHistory.service;

import com.plit.FO.matchHistory.dto.MatchSummaryDTO;
import com.plit.FO.matchHistory.dto.db.MatchHistoryDTO;
import com.plit.FO.matchHistory.dto.db.MatchOverallSummaryDTO;
import com.plit.FO.matchHistory.dto.riot.RiotParticipantDTO;
import com.plit.FO.matchHistory.entity.MatchPlayerEntity;
import com.plit.FO.matchHistory.entity.MatchSummaryEntity;
import com.plit.FO.matchHistory.repository.MatchOverallSummaryRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class MatchHelper { // 서브 메서드

    private static final Map<String, String> korNameMap = new HashMap<>();
    private static MatchOverallSummaryRepository matchPlayerRepository;

    // 한글 챔피언 이름 - riot 챔피언 json 으로 호출 - 모든 챔피언의 영어 이름(key) 과 한글 이름 필드가 들어있음
    @PostConstruct
    public void loadKorChampionMap() {
        try {
            String url = "https://ddragon.leagueoflegends.com/cdn/14.12.1/data/ko_KR/champion.json";
            RestTemplate restTemplate = new RestTemplate();
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            Map<String, Object> data = (Map<String, Object>) response.get("data");

            // korNameMap 에 매핑 저장
            for (String engName : data.keySet()) {
                Map<String, Object> champData = (Map<String, Object>) data.get(engName);
                String korName = (String) champData.get("name");
                korNameMap.put(engName, korName);
            }

            System.out.println("챔피언 한글 이름 불러오기 완료 (" + korNameMap.size() + "개)");
        } catch (Exception e) {
            System.err.println("챔피언 한글 이름 로딩 실패: " + e.getMessage());
        }
    }

    // 한글 챔피언 이름 조회
    public static String getKorName(String engName) {
        return korNameMap.getOrDefault(engName, engName);
    }


//    정규화
    public static String normalizePosition(String pos) {
        if (pos == null) return "unknown";
        return switch (pos.toUpperCase()) {
            case "TOP" -> "top";
            case "JUNGLE" -> "jungle";
            case "MID", "MIDDLE" -> "mid";
            case "ADC", "BOTTOM", "BOT" -> "bottom";
            case "SUPPORT", "UTILITY" -> "support";
            default -> "unknown";
        };
    }
    // 소문자화 + 띄어쓰기 제거
    public static String normalizeGameName(String gameName) {
        return gameName.trim().replaceAll("\\s+", "").toLowerCase();
    }
    public static String normalizeTagLine(String tagLine) {
        return tagLine.trim().toLowerCase();
    }



    // Object 라면 -> int
    public static int toInt(Object obj) {
        if (obj instanceof Integer) return (Integer) obj;
        if (obj instanceof Number) return ((Number) obj).intValue();
        if (obj instanceof String) {
            try {
                return Integer.parseInt((String) obj);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    // 반올림
    public static double round(double value, int precision) {
        return Math.round(value * Math.pow(10, precision)) / Math.pow(10, precision);
    }

    public static double getKda(int kills, int deaths, int assists) {
        if (deaths == 0) {
            return kills + assists;
        }
        return (double) (kills + assists) / deaths;
    }

    public static double getCsPerMin(int cs, int gameDurationSeconds) {
        if (gameDurationSeconds == 0) return 0;
        return cs / (gameDurationSeconds / 60.0);
    }

    // 킬 참여율
    public static double calculateKillParticipation(int kills, int assists, int teamTotalKills) {
        if (teamTotalKills == 0) return 0.0;
        return ((double)(kills + assists) / teamTotalKills) * 100;
    }

    // 팀 전체 킬 수
    public static int getTeamTotalKills(List<RiotParticipantDTO> participants, int teamId) {
        return participants.stream()
                .filter(p -> p.getTeamId() == teamId)
                .mapToInt(RiotParticipantDTO::getKills)
                .sum();
    }

    public static String getTimeAgo(LocalDateTime gameEnd) {
        Duration duration = Duration.between(gameEnd, LocalDateTime.now());

        long minutes = duration.toMinutes();
        long hours = duration.toHours();
        long days = duration.toDays();

        if (minutes < 1) return "방금 전";
        if (minutes < 60) return minutes + "분 전";
        if (hours < 24) return hours + "시간 전";
        return days + "일 전";
    }

    public static MatchOverallSummaryDTO getOverallSummary(String puuid, String gameName, String tagLine, List<MatchSummaryEntity> matches) {
        int totalMatches = matches.size();
        int totalWins = (int) matches.stream().filter(MatchSummaryEntity::isWin).count();
        int loseCount = totalMatches - totalWins;

        double avgKills = matches.stream().mapToInt(MatchSummaryEntity::getKills).average().orElse(0.0);
        double avgDeaths = matches.stream().mapToInt(MatchSummaryEntity::getDeaths).average().orElse(0.0);
        double avgAssists = matches.stream().mapToInt(MatchSummaryEntity::getAssists).average().orElse(0.0);
        double avgCs = matches.stream().mapToInt(MatchSummaryEntity::getCs).average().orElse(0.0);
        double avgKda = avgDeaths == 0 ? avgKills + avgAssists : (avgKills + avgAssists) / avgDeaths;
        double winRate = totalMatches == 0 ? 0.0 : (100.0 * totalWins / totalMatches);

        // 선호포지션
        String preferredPosition = matches.stream()
                .map(MatchSummaryEntity::getTeamPosition)
                .filter(pos -> pos != null && !pos.equals("NONE"))
                .collect(Collectors.groupingBy(p -> p, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("UNKNOWN");

        // 포지션별 개수
        Map<String, Long> positionCounts = matches.stream()
                .map(MatchSummaryEntity::getTeamPosition)
                .filter(pos -> pos != null && !pos.equals("NONE"))
                .collect(Collectors.groupingBy(pos -> pos, Collectors.counting()));

        // 포지션 별 비율
        Map<String, Double> favoritePositions = new HashMap<>();
        for (Map.Entry<String, Long> entry : positionCounts.entrySet()) {
            String pos = entry.getKey();
            Long count = entry.getValue();
            double percent = totalMatches == 0 ? 0.0 : (100.0 * count / totalMatches);
            favoritePositions.put(pos, round(percent, 1));
        }

        // 챔피언별 사용 횟수 카운트 후 상위 3개 추출
        List<String> preferredChampions = matches.stream()
                .map(MatchSummaryEntity::getChampionName)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(name -> name, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .toList();

        return MatchOverallSummaryDTO.builder()
                .puuid(puuid)
                .gameName(gameName)
                .tagLine(tagLine)
                .totalMatches(totalMatches)
                .totalWins(totalWins)
                .winRate(round(winRate,0))
                .averageKills(avgKills)
                .averageDeaths(avgDeaths)
                .averageAssists(avgAssists)
                .averageKda(avgKda)
                .averageCs(avgCs)
                .preferredPosition(preferredPosition)
                .favoritePositions(favoritePositions)
                .preferredChampions(preferredChampions)
                .positionCounts(positionCounts)
                .loseCount(loseCount)
                .build();
    }

    public static MatchOverallSummaryDTO convertToMatchOverallSummary(
            String puuid,
            String gameName,
            String tagLine,
            MatchSummaryDTO summary,
            List<MatchHistoryDTO> matchList
    ) {
        double averageCs = matchList.stream()
                .mapToInt(MatchHistoryDTO::getCs)
                .average()
                .orElse(0.0);

        Map<String, Long> positionCounts = matchList.stream()
                .map(MatchHistoryDTO::getTeamPosition)
                .filter(pos -> pos != null && !pos.equals("NONE"))
                .collect(Collectors.groupingBy(pos -> pos, Collectors.counting()));

        Map<String, Double> favoritePositions = new HashMap<>();
        for (Map.Entry<String, Long> entry : positionCounts.entrySet()) {
            String pos = entry.getKey();
            Long count = entry.getValue();
            double percent = summary.getTotalCount() == 0 ? 0.0 : (100.0 * count / summary.getTotalCount());
            favoritePositions.put(pos, round(percent, 1));
        }


        return MatchOverallSummaryDTO.builder()
                .puuid(puuid)
                .gameName(gameName)
                .tagLine(tagLine)
                .totalCount(summary.getTotalCount())
                .winCount(summary.getWinCount())
                .totalWins(summary.getWinCount()) // 중복 필드
                .totalMatches(summary.getTotalCount()) // 중복 필드
                .winRate(summary.getTotalCount() > 0
                        ? summary.getWinCount() * 100.0 / summary.getTotalCount()
                        : 0.0)
                .averageKills(summary.getAvgKills())
                .averageDeaths(summary.getAvgDeaths())
                .averageAssists(summary.getAvgAssists())
                .averageKda(summary.getKdaRatio())
                .averageCs(averageCs)
                .preferredPosition(summary.getSortedPositionList() != null && !summary.getSortedPositionList().isEmpty()
                        ? summary.getSortedPositionList().get(0)
                        : null)
                .favoritePositions(favoritePositions)
                .positionCounts(positionCounts)
                .preferredChampions(summary.getSortedChampionList() != null
                        ? summary.getSortedChampionList().stream().map(Map.Entry::getKey).toList()
                        : null)
                .championTotalGames(summary.getChampionTotalGames())
                .createdAt(LocalDateTime.now())
                .build();
    }


    public static List<String> splitString(String str) {
        return (str != null && !str.isBlank())
                ? Arrays.asList(str.split(","))
                : new ArrayList<>();
    }

}
