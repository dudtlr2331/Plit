package com.plit.FO.clan;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClanService {
    private final ClanRepository clanRepository;

    public List<ClanEntity> getAllClans() {
        return clanRepository.findAllByOrderByCreatedAtDesc();
    }

    public void createClan(ClanEntity clan) {
        clanRepository.save(clan);
    }

    public ClanEntity getClanById(Long id) {
        return clanRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 클랜입니다."));
    }

    public List<ClanEntity> searchClansByKeyword(String keyword) {
        return clanRepository.searchByTierAndKeyword(null, keyword != null && !keyword.isBlank() ? keyword : null);
    }

    public List<ClanEntity> filterByMinTier(String tier) {
        return clanRepository.searchByTierAndKeyword(tier != null && !tier.isBlank() ? tier : null, null);
    }

    public List<ClanEntity> searchClansByKeywordAndTier(String keyword, String tier) {
        List<ClanEntity> all = getAllClans();

        String lowerKeyword = keyword != null ? keyword.toLowerCase() : null;

        return all.stream()
                .filter(clan -> {
                    String clanTier = clan.getMinTier();

                    // 티어 필터 처리
                    boolean tierMatch = true;
                    if (tier != null && !tier.isBlank()) {
                        if (tier.equals("티어 전체")) {
                            tierMatch = true; // 전체 출력
                        } else {
                            tierMatch = tier.equals(clanTier); // 정확히 일치
                        }
                    }

                    // 키워드 필터 처리
                    boolean keywordMatch = true;
                    if (lowerKeyword != null && !lowerKeyword.isBlank()) {
                        keywordMatch =
                                (clan.getName() != null && clan.getName().toLowerCase().contains(lowerKeyword)) ||
                                        (clan.getIntro() != null && clan.getIntro().toLowerCase().contains(lowerKeyword)) ||
                                        (clanTier != null && clanTier.toLowerCase().contains(lowerKeyword));
                    }

                    return tierMatch && keywordMatch;
                })
                .toList();
    }
}
