package com.plit.FO.friend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "friend")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FriendEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "friends_no", nullable = false)
    private Integer friendsNo;

    @Column(name = "from_user_id", nullable = true)
    private Integer fromUserId;

    @Column(name = "to_user_id", nullable = true)
    private Integer toUserId;

    @Column(name = "status", nullable = true, length = 20)
    private String status;

    @Column(name = "created_at", nullable = true)
    private String createdAt;

    @Column(name = "memo", nullable = true)
    private String memo;
}
