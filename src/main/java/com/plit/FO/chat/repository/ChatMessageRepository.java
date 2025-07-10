package com.plit.FO.chat.repository;

import com.plit.FO.chat.entity.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {
    List<ChatMessageEntity> findByChatRoom_ChatRoomIdOrderBySentAtAsc(Long chatRoomId);
}
