package com.plit.FO.qna.inquirychat;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InquiryChatService {

    private final InquiryRoomRepository roomRepository;
    private final InquiryMessageRepository messageRepository;

    // 채팅방 생성 (이미 존재하면 재사용)
    public InquiryRoom createOrGetRoom(Long userId) {
        return roomRepository.findByUserId(userId)
                .orElseGet(() -> {
                    InquiryRoom room = new InquiryRoom();
                    room.setUserId(userId);
                    return roomRepository.save(room);
                });
    }

    // 채팅 메시지 저장
    public InquiryMessage saveMessage(Long roomId, Long senderId, String content) {
        InquiryMessage message = new InquiryMessage();
        message.setInquiryRoomId(roomId);
        message.setSenderId(senderId);
        message.setContent(content);
        return messageRepository.save(message);
    }

    // 채팅 메시지 조회
    public List<InquiryMessage> getMessages(Long roomId) {
        return messageRepository.findByInquiryRoomIdOrderBySentAtAsc(roomId);
    }

    // 채팅방 존재 확인 (없으면 생성)
    public void ensureRoomExists(Long roomId, Long userId) {
        boolean exists = roomRepository.existsById(roomId);
        if (!exists) {
            InquiryRoom room = new InquiryRoom();
            room.setInquiryRoomId(roomId);
            room.setUserId(userId);
            roomRepository.save(room);
        }
    }
}