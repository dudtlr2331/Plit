package com.plit.FO.chat.controller;

import com.plit.FO.chat.dto.ChatMessageDTO;
import com.plit.FO.chat.entity.ChatMessageEntity;
import com.plit.FO.chat.entity.ChatRoomEntity;
import com.plit.FO.chat.repository.ChatRoomRepository;
import com.plit.FO.chat.service.ChatRoomService;
import com.plit.FO.chat.service.ChatService;
import com.plit.FO.user.dto.UserDTO;
import com.plit.FO.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRestController {

    private final ChatService chatService;
    private final ChatRoomService chatRoomService;
    private final UserService userService;
    private final ChatRoomRepository chatRoomRepository;

    @GetMapping("/{roomId}")
    public List<ChatMessageDTO> getMessages(@PathVariable Long roomId) {
        List<ChatMessageEntity> messages = chatService.getMessagesByRoomId(roomId);

        return messages.stream().map(m -> {
            ChatMessageDTO dto = new ChatMessageDTO();
            dto.setRoomId(String.valueOf(roomId));
            dto.setSender(m.getSenderId());
            dto.setContent(m.getContent());
            dto.setSentAt(m.getSentAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

            Long senderId = m.getSenderId();
            String nickname = "알 수 없음";
            if (senderId != null) {
                nickname = userService.getUserBySeq(senderId.intValue())
                        .map(UserDTO::getUserNickname)
                        .orElse("알 수 없음");
            }
            dto.setSenderNickname(nickname);

            return dto;
        }).collect(Collectors.toList());
    }

    @GetMapping("/room/{userA}/{userB}")
    public ResponseEntity<Long> getOrCreateRoom(@PathVariable Long userA, @PathVariable Long userB) {
        ChatRoomEntity room = chatRoomService.getOrCreateFriendChatRoom(userA, userB);
        return ResponseEntity.ok(room.getChatRoomId());
    }

    @GetMapping("/rooms/unread/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getRoomsAndUnreadCount(@PathVariable Long userId) {
        List<ChatRoomEntity> rooms = chatRoomService.getChatRoomsByUserId(userId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (ChatRoomEntity room : rooms) {
            int unread = chatService.countUnreadMessages(room.getChatRoomId(), userId);

            // 사용자 ID 목록 가져오기
            List<Long> userIds = chatRoomService.getUserIdsInRoom(room.getChatRoomId());
            if (userIds.size() != 2) continue; // 1:1 채팅 아니면 스킵

            Long userA = userIds.get(0);
            Long userB = userIds.get(1);

            // 프론트에서 사용하는 문자열 형식으로 매핑
            String calculatedRoomId = (userA < userB)
                    ? userA + "_" + userB
                    : userB + "_" + userA;

            Map<String, Object> map = new HashMap<>();
            map.put("roomId", calculatedRoomId); // 문자열로 변경
            map.put("unreadCount", unread);
            result.add(map);
        }

        return ResponseEntity.ok(result);
    }


    @GetMapping("/rooms/{userId}")
    public ResponseEntity<List<Map<String, Object>>> getRoomsByUserId(@PathVariable Long userId) {
        List<ChatRoomEntity> rooms = chatRoomService.getChatRoomsByUserId(userId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (ChatRoomEntity room : rooms) {
            int unread = chatService.countUnreadMessages(room.getChatRoomId(), userId);

            Map<String, Object> map = new HashMap<>();
            map.put("roomId", room.getChatRoomId());
            map.put("unreadCount", unread);

            if ("friend".equals(room.getChatRoomType())) {
                // 기존 친구채팅 로직
                List<Long> userIds = chatRoomService.getUserIdsInRoom(room.getChatRoomId());
                Long otherId = userIds.stream().filter(id -> !id.equals(userId)).findFirst().orElse(null);
                if (otherId != null) {
                    UserDTO otherUser = userService.getUserBySeq(otherId.intValue())
                            .orElseThrow(() -> new RuntimeException("상대 유저 정보를 찾을 수 없습니다."));
                    map.put("type", "friend");
                    map.put("otherUserId", otherId);
                    map.put("otherNickname", otherUser.getUserNickname());
                }
            } else if ("party".equals(room.getChatRoomType())) {
                // 파티채팅 로직 추가
                map.put("type", "party");
                map.put("partyName", room.getChatRoomName()); // partyName으로 저장해둔 값
                map.put("partyId", room.getPartyId());
            }

            result.add(map);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/room/party/{partyId}")
    public ResponseEntity<Long> getPartyChatRoom(@PathVariable Long partyId) {
        ChatRoomEntity room = chatRoomRepository.findAll().stream()
                .filter(r -> "party".equals(r.getChatRoomType()) && partyId.equals(r.getPartyId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("채팅방이 없습니다."));
        return ResponseEntity.ok(room.getChatRoomId());
    }

    // 특정 채팅방의 메시지를 읽음 처리
    @PostMapping("/{roomId}/read")
    public ResponseEntity<Void> markMessagesAsRead(
            @PathVariable Long roomId,
            Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserDTO loginUser = userService.getUserByUserId(principal.getName())
                .orElse(null);
        if (loginUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        chatService.markMessagesAsRead(roomId, loginUser.getUserSeq().longValue());
        return ResponseEntity.ok().build();
    }

}
