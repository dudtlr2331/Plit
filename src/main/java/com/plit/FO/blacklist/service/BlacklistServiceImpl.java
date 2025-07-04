package com.plit.FO.blacklist.service;

import com.plit.FO.blacklist.dto.BlacklistDTO;
import com.plit.FO.blacklist.entity.BlacklistEntity;
import com.plit.FO.blacklist.repository.BlacklistRepository;
import com.plit.FO.user.UserDTO;
import com.plit.FO.user.UserEntity;
import com.plit.FO.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BlacklistServiceImpl implements BlacklistService {

    private final BlacklistRepository blacklistRepository;
    private final UserRepository userRepository; // User 닉네임 → ID 매핑용

    @Transactional
    public void report(BlacklistDTO dto, UserDTO reporter) {
        // 신고 대상 유저 닉네임 → ID 매핑
        UserEntity reportedUser = userRepository.findByUserNickname(dto.getReportedNickname())
                .orElseThrow(() -> new IllegalArgumentException("신고 대상 유저를 찾을 수 없습니다."));

        // 이미 테이블에 신고자와 피신고자의 정보가 동일하게 들어있으면 중복
        boolean alreadyReported = blacklistRepository.existsByReporterIdAndReportedUserId(reporter.getUserSeq(), reportedUser.getUserSeq());
        if (alreadyReported) {
            throw new IllegalArgumentException("이미 신고한 유저입니다.");
        }

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

        // 상태가 ACCEPTED인 경우 해당 유저 is_banned 처리
        if ("ACCEPTED".equals(status)) {
            UserEntity reportedUser = userRepository.findById(entity.getReportedUserId())
                    .orElseThrow(() -> new RuntimeException("신고당한 유저 정보를 찾을 수 없습니다."));
            reportedUser.setIsBanned(true);
            userRepository.save(reportedUser);
        }
    }


    // 트롤 신고 관리 데이터
    public List<BlacklistDTO> getAllReportsWithCount(Integer currentUserSeq) {
        List<BlacklistEntity> entities = blacklistRepository.findAllByOrderByReportedAtDesc();

        return entities.stream().map(entity -> {
            BlacklistDTO dto = new BlacklistDTO();

            // 기본 정보
            dto.setBlackListNo(entity.getBlacklistNo());
            dto.setReporterId(entity.getReporterId());
            dto.setReportedUserId(entity.getReportedUserId());
            dto.setReason(entity.getReason());
            dto.setStatus(entity.getStatus());
            dto.setReportedAt(entity.getReportedAt());
            dto.setHandledAt(entity.getHandledAt());
            dto.setHandledBy(entity.getHandledBy());

            // 닉네임 매핑
            String reporterNickname = userRepository.findById(entity.getReporterId())
                    .map(UserEntity::getUserNickname)
                    .orElse("알 수 없음");

            String reportedNickname = userRepository.findById(entity.getReportedUserId())
                    .map(UserEntity::getUserNickname)
                    .orElse("알 수 없음");

            String handledByNickname = (entity.getHandledBy() != null)
                    ? userRepository.findById(entity.getHandledBy())
                    .map(UserEntity::getUserNickname)
                    .orElse("알 수 없음")
                    : null;

            dto.setReporterNickname(reporterNickname);
            dto.setReportedNickname(reportedNickname);
            dto.setHandledByNickname(handledByNickname);

            // 신고당한 횟수 조회
            int count = blacklistRepository.countByReportedUserId(entity.getReportedUserId());
            dto.setReportedCount(count);

            // 시간 계산 (초/분/시간/일 단위)
            try {
                LocalDateTime reportedTime = LocalDateTime.parse(entity.getReportedAt(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                Duration duration = Duration.between(reportedTime, LocalDateTime.now());
                long seconds = duration.getSeconds();

                String timeAgo;
                if (seconds < 60) {
                    timeAgo = seconds + "초 전";
                } else if (seconds < 3600) {
                    timeAgo = (seconds / 60) + "분 전";
                } else if (seconds < 86400) {
                    timeAgo = (seconds / 3600) + "시간 전";
                } else {
                    timeAgo = (seconds / 86400) + "일 전";
                }

                dto.setTimeAgo(timeAgo);
            } catch (Exception e) {
                dto.setTimeAgo("알 수 없음");
            }


            // 동일 인물을 신고했는지 여부
            boolean alreadyReported = entity.getReporterId().equals(currentUserSeq);
            dto.setAlreadyReportedByCurrentUser(alreadyReported);

            return dto;
        }).collect(Collectors.toList());
    }

}