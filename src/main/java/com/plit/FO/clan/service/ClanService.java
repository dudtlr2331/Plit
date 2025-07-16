package com.plit.FO.clan.service;

import com.plit.FO.clan.dto.ClanDTO;
import com.plit.FO.clan.entity.ClanEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ClanService {
    List<ClanEntity> getAllClans();

    void createClan(ClanEntity clan);

    ClanEntity getClanById(Long id);

    List<ClanEntity> searchClansByKeyword(String keyword);

    List<ClanEntity> filterByMinTier(String tier);

    List<ClanEntity> searchClansByKeywordAndTier(String keyword, String tier);

    boolean isMember(Long clanId, Long userId);

    void deleteClan(Long id);

    boolean existsByNameAndUseYn(String name, String useYn);

    void updateClan(Long id, ClanEntity updatedClan, MultipartFile imageFile) throws IOException;

    ClanDTO findById(Long id);

    List<ClanDTO> getTop3ClansByMemberCount();


}