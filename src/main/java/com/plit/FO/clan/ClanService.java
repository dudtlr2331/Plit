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
        List<String> tierOrder = List.of("언랭크", "브론즈", "실버", "골드", "플레티넘", "다이아", "마스터", "챌린저");

        String lowerKeyword = keyword != null ? keyword.toLowerCase() : null;
        int tierIndexFromDropdown = tier != null ? tierOrder.indexOf(tier) : -1;
        int tierIndexFromKeyword = lowerKeyword != null && tierOrder.contains(lowerKeyword) ? tierOrder.indexOf(lowerKeyword) : -1;

        return all.stream()
                .filter(clan -> {
                    // 티어 필터링 (선택된 티어 필터 기준)
                    boolean tierMatch = true;
                    if (tierIndexFromDropdown != -1) {
                        int clanTierIndex = tierOrder.indexOf(clan.getMinTier());
                        tierMatch = clanTierIndex >= tierIndexFromDropdown;
                    }

                    // 키워드 필터링 (이름, 소개, 티어까지 포함)
                    boolean keywordMatch = true;
                    if (lowerKeyword != null && !lowerKeyword.isBlank()) {
                        keywordMatch =
                                (clan.getName() != null && clan.getName().toLowerCase().contains(lowerKeyword)) ||
                                        (clan.getIntro() != null && clan.getIntro().toLowerCase().contains(lowerKeyword)) ||
                                        (clan.getMinTier() != null && clan.getMinTier().toLowerCase().contains(lowerKeyword)) ||
                                        // 검색어가 티어면 상위 티어도 포함
                                        (tierIndexFromKeyword != -1 && tierOrder.indexOf(clan.getMinTier()) >= tierIndexFromKeyword);
                    }

                    return tierMatch && keywordMatch;
                })
                .toList();
    }
}
