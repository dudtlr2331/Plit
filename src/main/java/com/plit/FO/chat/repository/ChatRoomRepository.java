package com.plit.FO.chat.repository;

import com.plit.FO.chat.entity.ChatRoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoomEntity, Long> {
    Optional<ChatRoomEntity> findByChatRoomTypeAndChatRoomName(String chatRoomType, String chatRoomName);
    Optional<ChatRoomEntity> findByPartyId(Long partyId);
}
