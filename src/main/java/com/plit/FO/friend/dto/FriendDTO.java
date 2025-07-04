package com.plit.FO.friend.dto;

import com.plit.FO.user.UserDTO;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FriendDTO {
    private Integer friendsNo;
    private Integer fromUserId;
    private Integer toUserId;
    private String status;
    private String createAt;
    private String memo;

    // 친구 목록을 불러오거나 친구 신청이 들어왔을 때, 닉네임으로 상대가 누군지 식별하기 위해 필드 추가
    private UserDTO fromUser; // 친구 신청을 받은 사람이 신청을 한 사람의 정보를 가져올 때
    private UserDTO toUser; // 친구 신청을 한 사람의 요청이 수락된 후 친구목록이나 대화창에서 상대방의 정보를 가져올 때

    // userDTO 와 friendDTO 를 혼용할 때 사용하기 위함. 추가 당시에는 친구 목록 창에 닉네임과 메모를 함께 보여주기 위해
    private UserDTO user;
}
