package com.plit.FO.chat.service;

import com.plit.FO.chat.dto.ChatMessageDTO;
import com.plit.FO.chat.entity.ChatMessageEntity;
import com.plit.FO.chat.entity.ChatRoomEntity;
import com.plit.FO.chat.entity.ChatRoomUserEntity;
import com.plit.FO.chat.repository.ChatMessageRepository;
import com.plit.FO.chat.repository.ChatRoomRepository;
import com.plit.FO.chat.repository.ChatRoomUserRepository;
import com.plit.FO.user.dto.UserDTO;
import com.plit.FO.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserService userService;

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

    @Transactional
    public void markMessagesAsRead(Long roomId, Long userId) {
        List<ChatMessageEntity> unreadMessages =
                chatMessageRepository.findByChatRoom_ChatRoomIdAndSenderIdNotAndIsReadFalse(roomId, userId);

        for (ChatMessageEntity msg : unreadMessages) {
            msg.setIsRead(true);
        }

        chatMessageRepository.saveAll(unreadMessages);
    }


    public int countUnreadMessages(Long roomId, Long userId) {
        return chatMessageRepository.countByChatRoom_ChatRoomIdAndSenderIdNotAndIsReadFalse(roomId, userId);
    }

    public ChatMessageDTO toDTO(ChatMessageEntity entity, String senderNickname) {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setSender(entity.getSenderId());
        dto.setSenderNickname(senderNickname);
        dto.setContent(entity.getContent());
        dto.setSentAt(entity.getSentAt().format(DateTimeFormatter.ofPattern("HH:mm"))); // or yyyy-MM-dd HH:mm
        return dto;
    }

    public ChatMessageDTO saveMessage(ChatMessageDTO dto) {
        Long roomId = Long.parseLong(dto.getRoomId());
        ChatRoomEntity room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방이 존재하지 않습니다."));

        ChatMessageEntity saved = chatMessageRepository.save(ChatMessageEntity.builder()
                .chatRoom(room)
                .senderId(dto.getSender())
                .content(dto.getContent())
                .sentAt(LocalDateTime.now())
                .isRead(false)
                .build());

        String nickname = userService.getUserBySeq(dto.getSender().intValue())
                .map(UserDTO::getUserNickname)
                .orElse("알 수 없음");

        ChatMessageDTO result = new ChatMessageDTO();
        result.setRoomId(String.valueOf(saved.getChatRoom().getChatRoomId()));
        result.setSender(saved.getSenderId());
        result.setContent(saved.getContent());
        result.setSenderNickname(nickname);
        result.setSentAt(saved.getSentAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

        return result;
    }



}
