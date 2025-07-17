package com.plit.FO.clan.service;

import com.plit.FO.clan.dto.ClanDTO;
import com.plit.FO.clan.entity.ClanEntity;
import com.plit.FO.clan.entity.ClanMemberEntity;
import com.plit.FO.clan.enums.JoinStatus;
import com.plit.FO.clan.repository.ClanMemberRepository;
import com.plit.FO.clan.repository.ClanRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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

    // 예외2
    @Override
    public void createClan(ClanEntity clan) {
        clan.setUseYn("Y");
        ClanEntity saved = clanRepository.save(clan);

        if (saved.getLeaderId() != null) {
            try {
                ClanMemberEntity leader = ClanMemberEntity.builder()
                        .userId(saved.getLeaderId())
                        .clanId(saved.getId())
                        .role("LEADER")
                        .status(JoinStatus.APPROVED.name())
                        .intro("리더입니다")
                        .build();

                clanMemberRepository.save(leader);
            } catch (Exception e) {
                throw new RuntimeException("리더 자동 등록에 실패했습니다.", e);
            }
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

    // 예외3
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
            try {
                String fileName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();
                File dir = new File(uploadDir);
                if (!dir.exists()) dir.mkdirs();
                imageFile.transferTo(new File(dir, fileName));
                existing.setImageUrl("/upload/clan/" + fileName);
            } catch (IOException e) {
                throw new IOException("이미지 업로드 중 문제가 발생했습니다.", e);
            }
        }

        clanRepository.save(existing);
    }

    @Override
    public ClanDTO findById(Long id) {
        ClanEntity entity = clanRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 ID(" + id + ")에 해당하는 클랜이 없습니다."));

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

    @Override
    public List<ClanDTO> getTop3ClansByMemberCount() {
        return clanRepository.findAll().stream()
                .filter(clan -> "Y".equals(clan.getUseYn()))
                .map(clan -> {
                    int memberCount = clanMemberRepository.countByClanId(clan.getId());
                    return new ClanDTO(
                            clan.getId(),
                            clan.getName(),
                            clan.getIntro(),
                            clan.getKakaoLink(),
                            clan.getDiscordLink(),
                            clan.getMinTier(),
                            clan.getImageUrl(),
                            clan.getLeaderId(),
                            memberCount
                    );
                })
                .sorted((a, b) -> Integer.compare(b.getMemberCount(), a.getMemberCount()))
                .limit(3)
                .toList();
    }
}