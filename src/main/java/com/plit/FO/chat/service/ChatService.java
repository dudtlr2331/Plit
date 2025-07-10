package com.plit.FO.chat.service;

import com.plit.FO.chat.entity.ChatMessageEntity;
import com.plit.FO.chat.entity.ChatRoomEntity;
import com.plit.FO.chat.repository.ChatMessageRepository;
import com.plit.FO.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;

    public ChatMessageEntity saveMessage(Long roomId, Long senderId, String content) {
        ChatRoomEntity room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방이 존재하지 않습니다."));

        ChatMessageEntity message = ChatMessageEntity.builder()
                .chatRoom(room)
                .senderId(senderId)
                .content(content)
                .sentAt(LocalDateTime.now())
                .build();

        return chatMessageRepository.save(message);
    }

    public List<ChatMessageEntity> getMessagesByRoomId(Long roomId) {
        return chatMessageRepository.findByChatRoom_ChatRoomIdOrderBySentAtAsc(roomId);
    }
}
