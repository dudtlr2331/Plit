package com.plit.FO.blacklist.service;

import com.plit.FO.blacklist.dto.BlacklistDTO;
import com.plit.FO.user.UserDTO;

import java.util.List;

public interface BlacklistService {
    void report(BlacklistDTO dto, UserDTO reporter);
    void updateReportStatus(Integer blacklistNo, String status, Integer handlerId);
    List<BlacklistDTO> getAllReportsWithCount(Integer currentUserSeq);
}
