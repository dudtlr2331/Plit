package com.plit.FO.blacklist;

import com.plit.FO.user.UserDTO;
import com.plit.FO.user.UserEntity;
import com.plit.FO.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

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

    public void updateReportStatus(Integer blacklistNo, String status, Integer handlerId) {
        BlacklistEntity entity = blacklistRepository.findById(blacklistNo)
                .orElseThrow(() -> new RuntimeException("신고 기록을 찾을 수 없습니다."));

        entity.setStatus(status);
        entity.setHandledBy(handlerId);
        entity.setHandledAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        blacklistRepository.save(entity);
    }

    public List<BlacklistDTO> getAllReports() {
        List<BlacklistEntity> entities = blacklistRepository.findAll();

        return entities.stream().map(entity -> {
            BlacklistDTO dto = new BlacklistDTO();
            dto.setBlackListNo(entity.getBlacklistNo());
            dto.setReason(entity.getReason());
            dto.setStatus(entity.getStatus());
            dto.setReportedAt(entity.getReportedAt());
            dto.setHandledAt(entity.getHandledAt());

            // 유저 정보 매핑 (예: 닉네임)
            dto.setReporterNickname(userRepository.findById(entity.getReporterId())
                    .map(UserEntity::getUserNickname).orElse("알 수 없음"));
            dto.setReportedNickname(userRepository.findById(entity.getReportedUserId())
                    .map(UserEntity::getUserNickname).orElse("알 수 없음"));
            dto.setHandledByNickname(entity.getHandledBy() != null
                    ? userRepository.findById(entity.getHandledBy()).map(UserEntity::getUserNickname).orElse("-")
                    : null);

            return dto;
        }).collect(Collectors.toList());
    }
}