package com.plit.FO.blacklist;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlacklistDTO {
    private Integer blackListNo;
    private Integer reporterId;
    private Integer reportedUserId;
    private String reason;
    private String status;
    private Integer handledBy;
    private String reportedAt;
    private String handledAt;

    // 신고자 닉네임
    private String reporterNickname;
    
    // 신고당한 유저 닉네임
    private String reportedNickname;

    // 처리자 닉네임
    private String handledByNickname;

    // 신고 횟수
    private Integer reportedCount;

    // 게시 후 지난 시간
    private String timeAgo;

    // 중복 신고 방지용 신고이력
    private boolean alreadyReportedByCurrentUser;
}
