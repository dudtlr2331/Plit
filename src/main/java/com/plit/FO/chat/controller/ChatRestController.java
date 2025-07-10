package com.plit.FO.chat.controller;

import com.plit.FO.chat.dto.ChatMessageDTO;
import com.plit.FO.chat.entity.ChatMessageEntity;
import com.plit.FO.chat.entity.ChatRoomEntity;
import com.plit.FO.chat.service.ChatRoomService;
import com.plit.FO.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRestController {

    private final ChatService chatService;
    private final ChatRoomService chatRoomService;

    @GetMapping("/{roomId}")
    public List<ChatMessageDTO> getMessages(@PathVariable Long roomId) {
        List<ChatMessageEntity> messages = chatService.getMessagesByRoomId(roomId);

        return messages.stream().map(m -> {
            ChatMessageDTO dto = new ChatMessageDTO();
            dto.setRoomId(String.valueOf(roomId));
            dto.setSender(String.valueOf(m.getSenderId()));
            dto.setContent(m.getContent());
            return dto;
        }).collect(Collectors.toList());
    }

    @GetMapping("/room/{userA}/{userB}")
    public ResponseEntity<Long> getOrCreateRoom(@PathVariable Long userA, @PathVariable Long userB) {
        ChatRoomEntity room = chatRoomService.getOrCreateFriendChatRoom(userA, userB);
        return ResponseEntity.ok(room.getChatRoomId());
    }
}
