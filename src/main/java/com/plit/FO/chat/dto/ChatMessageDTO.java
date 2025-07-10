package com.plit.FO.chat.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Repository;

@Repository
@Getter
@Setter
public class ChatMessageDTO {
    private String roomId;
    private Long sender;
    private String content;

    //채팅창에 표시하기 위한 닉네임, 보낸 시간
    private String senderNickname;
    private String sentAt;
}
