package com.plit.FO.clan.service;

import com.plit.FO.clan.entity.ClanEntity;
import com.plit.FO.clan.repository.ClanMemberRepository;
import com.plit.FO.clan.repository.ClanRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClanServiceImpl implements ClanService {

    private final ClanRepository clanRepository;
    private final ClanMemberRepository clanMemberRepository;

    @Override
    public List<ClanEntity> getAllClans() {
        return clanRepository.findByUseYnOrderByCreatedAtDesc("Y");
    }

    @Override
    public void createClan(ClanEntity clan) {
        clan.setUseYn("Y"); // 보험!!
        clanRepository.save(clan);
    }

    @Override
    public ClanEntity getClanById(Long id) {
        return clanRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 클랜입니다."));
    }

    @Override
    public List<ClanEntity> searchClansByKeyword(String keyword) {
        return clanRepository.searchByTierAndKeyword(null, keyword != null && !keyword.isBlank() ? keyword : null);
    }

    @Override
    public List<ClanEntity> filterByMinTier(String tier) {
        return clanRepository.searchByTierAndKeyword(tier != null && !tier.isBlank() ? tier : null, null);
    }

    @Override
    public List<ClanEntity> searchClansByKeywordAndTier(String keyword, String tier) {
        List<ClanEntity> all = getAllClans();

        String lowerKeyword = keyword != null ? keyword.toLowerCase() : null;

        return all.stream()
                .filter(clan -> {
                    String clanTier = clan.getMinTier();

                    boolean tierMatch = true;
                    if (tier != null && !tier.isBlank()) {
                        if (tier.equals("티어 전체")) {
                            tierMatch = true;
                        } else {
                            tierMatch = tier.equals(clanTier);
                        }
                    }

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

    @Override
    public boolean isMember(Long clanId, Long userId) {
        return clanMemberRepository.existsByClanIdAndUserId(clanId, userId);
    }

    @Override
    public void deleteClan(Long id) {
        ClanEntity clan = clanRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 클랜입니다."));

        clan.setUseYn("N");
        clanRepository.save(clan);
    }

    @Override
    public boolean existsByNameAndUseYn(String name, String useYn) {
        return clanRepository.existsByNameAndUseYn(name, useYn);
    }
}