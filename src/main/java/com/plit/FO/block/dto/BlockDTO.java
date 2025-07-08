package com.plit.FO.block.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockDTO {
    private Integer no;
    private Integer blockerId;
    private Integer blockedUserId;
    private LocalDateTime blockedAt;
    private Boolean isReleased;

    private String blockedUserNickname;
}
