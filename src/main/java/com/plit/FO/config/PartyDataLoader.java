package com.plit.FO.config;

import com.plit.FO.party.PartyEntity;
import com.plit.FO.party.PartyRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
public class PartyDataLoader {

    @Bean
    CommandLineRunner loadData(PartyRepository repository) {
        return args -> {
            if (repository.count() == 0) {

                // 데이터가 없을 때만 insert
                repository.save(PartyEntity.builder()
                        .partyName("첫 번째 파티")
                        .partyType("TYPE1")
                        .partyCreateDate(LocalDateTime.now())
                        .partyEndTime(LocalDateTime.now().plusDays(1))
                        .partyStatus("WAITING")
                        .partyHeadcount(1)
                        .partyMax(5)
                        .build());

                repository.save(PartyEntity.builder()
                        .partyName("두 번째 파티")
                        .partyType("TYPE2")
                        .partyCreateDate(LocalDateTime.now())
                        .partyEndTime(LocalDateTime.now().plusDays(2))
                        .partyStatus("FULL")
                        .partyHeadcount(5)
                        .partyMax(5)
                        .build());
            }
        };
    }
}