package com.plit.FO.blacklist;

import com.plit.FO.user.UserDTO;
import com.plit.FO.user.UserEntity;
import com.plit.FO.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class BlacklistService {

    private final BlacklistRepository blacklistRepository;
    private final UserRepository userRepository; // User 닉네임 → ID 매핑용

    @Transactional
    public void report(BlacklistDTO dto, UserDTO reporter) {
        // 신고 대상 유저 닉네임 → ID 매핑
        UserEntity reportedUser = userRepository.findByUserNickname(dto.getReportedNickname())
                .orElseThrow(() -> new IllegalArgumentException("신고 대상 유저를 찾을 수 없습니다."));

        BlacklistEntity entity = BlacklistEntity.builder()
                .reporterId(reporter.getUserSeq())
                .reportedUserId(reportedUser.getUserSeq())
                .reason(dto.getReason())
                .status("PENDING")  // 초기 상태
                .reportedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .build();

        blacklistRepository.save(entity);
    }
}