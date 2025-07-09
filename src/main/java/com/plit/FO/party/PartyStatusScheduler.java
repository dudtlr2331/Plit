package com.plit.FO.party;

import com.plit.FO.party.entity.PartyEntity;
import com.plit.FO.party.enums.PartyStatus;
import com.plit.FO.party.repository.PartyRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PartyStatusScheduler {

    private final PartyRepository partyRepository;

    @Scheduled(fixedRate = 1 * 60 * 1000) // 1분마다 실행
    @Transactional
    public void updatePartyStatuses() {
        LocalDateTime now = LocalDateTime.now();
        List<PartyEntity> parties = partyRepository.findAll();

        for (PartyEntity party : parties) {
            PartyStatus beforeStatus = party.getPartyStatus();

            if (now.isAfter(party.getPartyEndTime())) {
                party.setPartyStatus(PartyStatus.CLOSED);
            } else if (party.getPartyHeadcount() >= party.getPartyMax()) {
                party.setPartyStatus(PartyStatus.FULL);
            } else {
                party.setPartyStatus(PartyStatus.WAITING);
            }

            if (beforeStatus != party.getPartyStatus()) {
                log.info("파티 상태 변경: {} → {} (파티ID: {})",
                        beforeStatus.name(), party.getPartyStatus().name(), party.getPartySeq());
            }
        }
    }
}