package com.plit.FO.matchHistory.service;

import com.plit.FO.matchHistory.entity.MatchPlayerEntity;
import com.plit.FO.matchHistory.entity.MatchSummaryEntity;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class MatchHelper { // 서브 메서드

    private static final Map<String, String> korNameMap = new HashMap<>();

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

    // 몇 일, 몇 시간, 몇 분 전 매치였는지
    public static String getTimeAgo(LocalDateTime gameEndTime) {
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(gameEndTime, now);

        long days = duration.toDays();
        long hours = duration.toHours();
        long minutes = duration.toMinutes();

        if (days > 0) {
            return days + "일 전";
        } else if (hours > 0) {
            return hours + "시간 전";
        } else if (minutes > 0) {
            return minutes + "분 전";
        } else {
            return "방금 전";
        }
    }





}
