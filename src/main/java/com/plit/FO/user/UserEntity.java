package com.plit.FO.user;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_seq", nullable = false)
    private Integer userSeq;

    @Column(name = "user_id", nullable = false, unique = true, length = 16)
    private String userId;

    @Column(name = "user_pwd", nullable = false, length = 24)
    private String userPwd; // In real app, store hashed password

    @Column(name = "user_nickname", nullable = false, unique = true, length = 30)
    private String userNickname;

    @Column(name = "use_yn", nullable = false, length = 1)
    private String useYn;

    @Column(name = "is_banned", nullable = false)
    private Boolean isBanned;

    @Column(name = "user_modi_id", nullable = false, length = 16)
    private String userModiId;

    @Column(name = "user_modi_date", nullable = false)
    private LocalDate userModiDate;

    @Column(name = "user_create_date", nullable = false)
    private LocalDate userCreateDate;

    @Column(name = "user_auth", nullable = false, length = 5)
    private String userAuth;
}