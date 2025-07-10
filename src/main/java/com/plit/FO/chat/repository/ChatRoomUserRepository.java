package com.plit.FO.chat.repository;

import com.plit.FO.chat.entity.ChatRoomUserEntity;
import com.plit.FO.chat.entity.ChatRoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomUserRepository extends JpaRepository<ChatRoomUserEntity, Long> {
    boolean existsByChatRoom_ChatRoomIdAndUserId(Long chatRoomId, Long userId);

    Optional<ChatRoomUserEntity> findByChatRoom_ChatRoomIdAndUserId(Long chatRoomId, Long userId);
    List<ChatRoomUserEntity> findByUserId(Long userId);
}
