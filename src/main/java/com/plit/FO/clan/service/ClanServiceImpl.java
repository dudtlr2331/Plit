package com.plit.FO.clan.service;

import com.plit.FO.clan.dto.ClanDTO;
import com.plit.FO.clan.entity.ClanEntity;
import com.plit.FO.clan.entity.ClanMemberEntity;
import com.plit.FO.clan.enums.JoinStatus;
import com.plit.FO.clan.repository.ClanMemberRepository;
import com.plit.FO.clan.repository.ClanRepository;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClanServiceImpl implements ClanService {

    private final ClanRepository clanRepository;
    private final ClanMemberRepository clanMemberRepository;

    @Override
    public List<ClanEntity> getAllClans() {
        try {
            return clanRepository.findByUseYnOrderByCreatedAtDesc("Y");
        } catch (Exception e) {
            // 로그만 찍고 빈 리스트 반환
            System.err.println("클랜 목록 조회 중 오류 발생: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public void createClan(ClanEntity clan) {
        clan.setUseYn("Y");

        ClanEntity saved;
        try {
            saved = clanRepository.save(clan);
        } catch (Exception e) {
            throw new RuntimeException("클랜 저장에 실패했습니다.", e);
        }

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
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 클랜입니다. (id=" + id + ")"));
    }

    @Override
    public List<ClanEntity> searchClansByKeyword(String keyword) {
        try {
            String sanitizedKeyword = (keyword != null && !keyword.isBlank()) ? keyword : null;
            return clanRepository.searchByTierAndKeyword(null, sanitizedKeyword);
        } catch (Exception e) {
            throw new RuntimeException("클랜 검색 중 오류가 발생했습니다. 입력값: " + keyword, e);
        }
    }

    @Override
    public List<ClanEntity> filterByMinTier(String tier) {
        try {
            String sanitizedTier = (tier != null && !tier.isBlank()) ? tier : null;
            return clanRepository.searchByTierAndKeyword(sanitizedTier, null);
        } catch (Exception e) {
            throw new RuntimeException("클랜 필터링 중 오류가 발생했습니다. tier: " + tier, e);
        }
    }

    @Override
    public List<ClanEntity> searchClansByKeywordAndTier(String keyword, String tier) {
        try {
            List<ClanEntity> all = getAllClans();
            String lowerKeyword = keyword != null ? keyword.toLowerCase() : null;

            return all.stream()
                    .filter(clan -> {
                        String clanTier = clan.getMinTier();

                        boolean tierMatch = true;
                        if (tier != null && !tier.isBlank()) {
                            if (!"티어 전체".equals(tier)) {
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

        } catch (Exception e) {
            throw new RuntimeException("클랜 검색 중 오류가 발생했습니다. [keyword=" + keyword + ", tier=" + tier + "]", e);
        }
    }

    @Override
    public boolean isMember(Long clanId, Long userId) {
        if (clanId == null || userId == null) {
            throw new IllegalArgumentException("clanId와 userId는 null일 수 없습니다.");
        }

        try {
            return clanMemberRepository.existsByClanIdAndUserId(clanId, userId);
        } catch (Exception e) {
            throw new RuntimeException("클랜 멤버 여부 확인 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public void deleteClan(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("클랜 ID는 null일 수 없습니다.");
        }

        try {
            ClanEntity clan = clanRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 클랜입니다."));

            clan.setUseYn("N"); // 소프트 삭제
            clanRepository.save(clan);
        } catch (Exception e) {
            throw new RuntimeException("클랜 삭제 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public boolean existsByNameAndUseYn(String name, String useYn) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("클랜 이름은 비어 있을 수 없습니다.");
        }

        if (useYn == null || useYn.isBlank()) {
            throw new IllegalArgumentException("사용 여부(useYn)는 비어 있을 수 없습니다.");
        }

        return clanRepository.existsByNameAndUseYn(name, useYn);
    }

    @Value("${custom.upload-path.clan}")
    private String uploadDir;

    @PostConstruct
    public void validateUploadDir() {
        if (uploadDir == null || uploadDir.isBlank()) {
            throw new IllegalStateException("custom.upload-path.clan 설정값이 없습니다. application.yml 확인 필요!");
        }

        File dir = new File(uploadDir);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                throw new IllegalStateException("클랜 이미지 업로드 경로 생성 실패: " + uploadDir);
            }
        }
    }

    @Override
    @Transactional
    public void updateClan(Long id, ClanEntity updatedClan, MultipartFile imageFile) throws IOException {
        ClanEntity existing = clanRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("클랜을 찾을 수 없습니다."));

        try {
            // 기본 정보 수정
            existing.setIntro(updatedClan.getIntro());
            existing.setMinTier(updatedClan.getMinTier());
            existing.setKakaoLink(updatedClan.getKakaoLink());
            existing.setDiscordLink(updatedClan.getDiscordLink());

            // 이미지 업로드 처리
            if (imageFile != null && !imageFile.isEmpty()) {
                String originalFilename = imageFile.getOriginalFilename();
                if (originalFilename == null || originalFilename.contains("..")) {
                    throw new IllegalArgumentException("잘못된 이미지 파일명입니다.");
                }

                String fileName = UUID.randomUUID() + "_" + originalFilename;
                File dir = new File(uploadDir);
                if (!dir.exists() && !dir.mkdirs()) {
                    throw new IllegalStateException("이미지 업로드 폴더 생성에 실패했습니다: " + uploadDir);
                }

                File destination = new File(dir, fileName);
                imageFile.transferTo(destination);

                existing.setImageUrl("/upload/clan/" + fileName);
            }

            clanRepository.save(existing);
        } catch (IOException e) {
            throw new IOException("이미지 업로드 중 문제가 발생했습니다.", e);
        } catch (Exception e) {
            throw new RuntimeException("클랜 정보 수정 중 예외가 발생했습니다.", e);
        }
    }

    @Override
    public ClanDTO findById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("유효하지 않은 클랜 ID입니다.");
        }

        ClanEntity entity = clanRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 ID(" + id + ")에 해당하는 클랜이 없습니다."));

        int count;
        try {
            count = clanMemberRepository.countByClanId(entity.getId());
        } catch (Exception e) {
            throw new RuntimeException("클랜 멤버 수 조회 중 오류가 발생했습니다.", e);
        }

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
        try {
            return clanRepository.findAll().stream()
                    .filter(clan -> "Y".equals(clan.getUseYn()))
                    .map(clan -> {
                        int memberCount = 0;
                        try {
                            memberCount = clanMemberRepository.countByClanId(clan.getId());
                        } catch (Exception e) {
                            System.err.println("클랜 멤버 수 조회 실패: clanId = " + clan.getId());
                        }

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
        } catch (Exception e) {
            throw new RuntimeException("Top3 클랜 조회 중 문제가 발생했습니다.", e);
        }
    }
}