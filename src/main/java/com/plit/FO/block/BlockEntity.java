package com.plit.FO.block;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "blocked_user")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlockEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "no", nullable = false)
    private Integer no;

    @Column(name = "blocker_id")
    private Integer blockerId;

    @Column(name = "blocked_user_id")
    private Integer blockedUserId;

    @Column(name = "blocked_at")
    private LocalDateTime blockedAt;

    @Column(name = "is_released")
    private Boolean isReleased;
}
