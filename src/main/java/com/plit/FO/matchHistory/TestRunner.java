package com.plit.FO.matchHistory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

// 테스트용

@Component
public class TestRunner implements CommandLineRunner {

    @Value("${riot.api.key}")
    private String apiKey;

    @Override
    public void run(String... args) {
        String gameName = "HideOnBush";
        String tagLine = "KR1";
        String url = "https://asia.api.riotgames.com/riot/account/v1/accounts/by-riot-id/"
                + gameName + "/" + tagLine + "?api_key=" + apiKey;

        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(null),
                    String.class
            );
            System.out.println("API 호출 성공:\n" + response.getBody());
        } catch (Exception e) {
            System.err.println("API 호출 실패: " + e.getMessage());
        }
    }
}

