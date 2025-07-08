package com.plit.FO.blacklist.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "blacklist")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlacklistEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "blacklist_no", nullable = false)
    private Integer blacklistNo;

    @Column(name = "reporter_id")
    private Integer reporterId;

    @Column(name = "reported_user_id")
    private Integer reportedUserId;

    @Column(name = "reason")
    private String reason;

    @Column(name = "status")
    private String status;

    @Column(name = "handled_by")
    private Integer handledBy;

    @Column(name = "reported_at")
    private String reportedAt;

    @Column(name = "handled_at")
    private String handledAt;
}
