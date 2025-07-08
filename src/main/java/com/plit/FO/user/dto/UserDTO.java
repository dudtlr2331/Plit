package com.plit.FO.user.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private Integer userSeq;
    private String userId;
    private String userPwd;
    private String userNickname;
    private String useYn;
    private Boolean isBanned;
    private String userModiId;
    private LocalDate userModiDate;
    private LocalDate userCreateDate;
    private String userAuth;
}