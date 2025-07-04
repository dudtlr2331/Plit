package com.plit.FO.block.service;

import com.plit.FO.block.dto.BlockDTO;

import java.util.List;

public interface BlockService {
    List<BlockDTO> getBlockedUsersByBlockerId(Integer currentUserId);
    void releaseBlock(Integer blockNo);
}
