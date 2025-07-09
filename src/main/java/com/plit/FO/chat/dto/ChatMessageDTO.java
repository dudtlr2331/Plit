package com.plit.FO.chat.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Repository;

@Repository
@Getter
@Setter
public class ChatMessageDTO {
    private String roomId;
    private String sender;
    private String content;
}
