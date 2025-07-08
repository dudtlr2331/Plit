package com.plit.BO.qna.inquiryChat;

import com.plit.FO.qna.inquirychat.InquiryMessage;
import com.plit.FO.qna.inquirychat.InquiryMessageRepository;
import com.plit.FO.qna.inquirychat.InquiryRoom;
import com.plit.FO.qna.inquirychat.InquiryRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BoInquiryChatService {

    private final InquiryRoomRepository roomRepository;
    private final InquiryMessageRepository messageRepository;

    // 관리자가 채팅방에 배정되도록
    public InquiryRoom assignAdminToRoom(Long roomId, Long adminId) {
        InquiryRoom room = roomRepository.findById(roomId).orElseThrow();
        room.setAdminId(adminId);
        return roomRepository.save(room);
    }

    // 채팅 메시지 전체 조회
    public List<InquiryMessage> getMessages(Long roomId) {
        return messageRepository.findByInquiryRoomIdOrderBySentAtAsc(roomId);
    }

    // 채팅 메시지 저장
    public InquiryMessage saveMessage(Long roomId, Long senderId, String content) {
        InquiryMessage message = new InquiryMessage();
        message.setInquiryRoomId(roomId);
        message.setSenderId(senderId);
        message.setContent(content);
        return messageRepository.save(message);
    }
    // 아직 응답하지 않은(관리자가 배정되지 않은) 채팅방 리스트
    public List<InquiryRoom> getPendingRooms() {
        return roomRepository.findByAdminIdIsNullOrderByCreatedAtAsc();
    }
}