package com.plit.FO.chat.service;

import com.plit.FO.chat.entity.ChatRoomEntity;
import com.plit.FO.chat.entity.ChatRoomUserEntity;
import com.plit.FO.chat.repository.ChatRoomRepository;
import com.plit.FO.chat.repository.ChatRoomUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomUserRepository chatRoomUserRepository;

    public ChatRoomEntity getOrCreateFriendChatRoom(Long userA, Long userB) {
        // 방 이름을 항상 정렬된 userId 기준으로 생성
        String roomName = generateFriendRoomName(userA, userB);

        // 기존에 있는 채팅방 탐색
        Optional<ChatRoomEntity> existingRoom = chatRoomRepository
                .findAll()
                .stream()
                .filter(room -> "friend".equals(room.getChatRoomType()) &&
                        roomName.equals(room.getChatRoomName()))
                .findFirst();

        if (existingRoom.isPresent()) return existingRoom.get();

        // 채팅방 생성
        ChatRoomEntity room = ChatRoomEntity.builder()
                .chatRoomType("friend")
                .chatRoomName(roomName)
                .chatRoomMax(2)
                .chatRoomHeadcount(2)
                .chatRoomCreatedAt(LocalDateTime.now())
                .build();
        ChatRoomEntity savedRoom = chatRoomRepository.save(room);

        // 참여자 등록
        ChatRoomUserEntity user1 = ChatRoomUserEntity.builder()
                .chatRoom(savedRoom)
                .userId(userA)
                .joinedAt(LocalDateTime.now())
                .build();

        ChatRoomUserEntity user2 = ChatRoomUserEntity.builder()
                .chatRoom(savedRoom)
                .userId(userB)
                .joinedAt(LocalDateTime.now())
                .build();

        chatRoomUserRepository.save(user1);
        chatRoomUserRepository.save(user2);

        return savedRoom;
    }

    public String generateFriendRoomName(Long userA, Long userB) {
        return (userA < userB)
                ? "user-" + userA + "_" + userB
                : "user-" + userB + "_" + userA;
    }

    public List<ChatRoomEntity> getChatRoomsByUserId(Long userId) {
        return chatRoomUserRepository.findByUserId(userId)
                .stream()
                .map(ChatRoomUserEntity::getChatRoom)
                .toList();
    }

    public List<Long> getUserIdsInRoom(Long chatRoomId) {
        return chatRoomUserRepository.findByChatRoom_ChatRoomId(chatRoomId)
                .stream()
                .map(ChatRoomUserEntity::getUserId)
                .toList();
    }

    public ChatRoomEntity createPartyChatRoom(Long partyId, String partyName) {
        ChatRoomEntity room = ChatRoomEntity.builder()
                .chatRoomType("party")
                .chatRoomName("party-" + partyId) // 필요하다면 이름도 지정
                .partyId(partyId)                 // 파티ID 연결
                .chatRoomMax(5)
                .chatRoomHeadcount(0)
                .chatRoomCreatedAt(LocalDateTime.now())
                .build();
        return chatRoomRepository.save(room);
    }

    public ChatRoomEntity getOrCreatePartyChatRoom(Long partyId, String partyName) {
        // 기존 방 있는지 찾기
        Optional<ChatRoomEntity> existingRoom = chatRoomRepository.findAll().stream()
                .filter(room -> "party".equals(room.getChatRoomType()) &&
                        room.getPartyId() != null &&
                        room.getPartyId().equals(partyId))
                .findFirst();

        if (existingRoom.isPresent()) {
            return existingRoom.get();
        }

        // 새로 만들기
        ChatRoomEntity room = ChatRoomEntity.builder()
                .chatRoomType("party")
                .chatRoomName(partyName)
                .chatRoomMax(5) // 파티 최대 인원
                .chatRoomHeadcount(1) // 파티장은 기본 1명
                .chatRoomCreatedAt(LocalDateTime.now())
                .partyId(partyId) // 🔥 여기서 partyId 세팅
                .build();

        return chatRoomRepository.save(room);
    }

}
