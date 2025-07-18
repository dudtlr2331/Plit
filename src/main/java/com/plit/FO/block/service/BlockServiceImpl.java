package com.plit.FO.block.service;

import com.plit.FO.block.dto.BlockDTO;
import com.plit.FO.block.entity.BlockEntity;
import com.plit.FO.block.repository.BlockRepository;
import com.plit.FO.user.entity.UserEntity;
import com.plit.FO.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BlockServiceImpl implements BlockService {

    private final BlockRepository blockRepository;
    private final UserRepository userRepository;

    public List<BlockDTO> getBlockedUsersByBlockerId(Integer currentUserId) {
        List<BlockEntity> blocks = blockRepository.findAllByBlockerIdAndIsReleasedFalse(currentUserId);

        return blocks.stream().map(block -> {
            UserEntity blockedUser = userRepository.findById(block.getBlockedUserId()).orElse(null);

            return BlockDTO.builder()
                    .no(block.getNo())
                    .blockerId(currentUserId)
                    .blockedUserId(blockedUser != null ? blockedUser.getUserSeq() : null)
                    .blockedUserNickname(blockedUser != null ? blockedUser.getUserNickname() : "(알 수 없음)")
                    .blockedAt(block.getBlockedAt())
                    .isReleased(false)
                    .build();
        }).collect(Collectors.toList());
    }

    public void releaseBlock(Integer blockNo) {
        BlockEntity block = blockRepository.findById(blockNo)
                .orElseThrow(() -> new IllegalArgumentException("차단 정보가 존재하지 않습니다."));

        block.setIsReleased(true);
        blockRepository.save(block); // 상태만 true로 변경
    }

    public void blockUser(Integer blockerId, Integer blockedUserId) {
        if (blockRepository.existsByBlockerIdAndBlockedUserIdAndIsReleasedFalse(blockerId, blockedUserId)) return;

        BlockEntity block = BlockEntity.builder()
                .blockerId(blockerId)
                .blockedUserId(blockedUserId)
                .blockedAt(LocalDateTime.now())
                .isReleased(false)
                .build();

        blockRepository.save(block);
    }
    // 다른 게시판에서 차단 기능을 사용하기 위한 이미 차단된 유저 판단
    public boolean isBlocked(Integer mySeq, Integer targetSeq) {
        return blockRepository.existsByBlockerIdAndBlockedUserIdAndIsReleased(mySeq, targetSeq, false);
    }

}
