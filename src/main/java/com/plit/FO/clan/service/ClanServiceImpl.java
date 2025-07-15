package com.plit.FO.clan.service;

import com.plit.FO.clan.dto.ClanDTO;
import com.plit.FO.clan.entity.ClanEntity;
import com.plit.FO.clan.entity.ClanMemberEntity;
import com.plit.FO.clan.repository.ClanMemberRepository;
import com.plit.FO.clan.repository.ClanRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
//import lombok.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

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
        clan.setUseYn("Y");
        ClanEntity saved = clanRepository.save(clan);

        // 리더 자동 등록
        if (saved.getLeaderId() != null) {
            ClanMemberEntity leader = ClanMemberEntity.builder()
                    .userId(saved.getLeaderId())
                    .clanId(saved.getId())
                    .role("LEADER")
                    .status("승인")
                    .intro("리더입니다 👑")
                    .build();
            clanMemberRepository.save(leader);
        }
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

    @Value("${custom.upload-path.clan}")
    private String uploadDir;

    @Override
    @Transactional
    public void updateClan(Long id, ClanEntity updatedClan, MultipartFile imageFile) throws IOException {
        ClanEntity existing = clanRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("클랜을 찾을 수 없습니다."));

        existing.setIntro(updatedClan.getIntro());
        existing.setMinTier(updatedClan.getMinTier());
        existing.setKakaoLink(updatedClan.getKakaoLink());
        existing.setDiscordLink(updatedClan.getDiscordLink());

        if (imageFile != null && !imageFile.isEmpty()) {
            String fileName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();
            imageFile.transferTo(new File(dir, fileName));
            existing.setImageUrl("/upload/clan/" + fileName);
        }

        clanRepository.save(existing);
    }

    @Override
    public ClanDTO findById(Long id) {
        ClanEntity entity = clanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("클랜을 찾을 수 없습니다."));

        int count = clanMemberRepository.countByClanId(entity.getId());

        return ClanDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .intro(entity.getIntro())
                .imageUrl(entity.getImageUrl())
                .minTier(entity.getMinTier())
                .discordLink(entity.getDiscordLink())
                .kakaoLink(entity.getKakaoLink())
                .leaderId(entity.getLeaderId())
                .memberCount(count)
                .build();
    }
}