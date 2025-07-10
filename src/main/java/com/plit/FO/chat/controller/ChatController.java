package com.plit.FO.chat.controller;

import com.plit.FO.chat.dto.ChatMessageDTO;
import com.plit.FO.chat.service.ChatService;
import com.plit.FO.user.dto.UserDTO;
import com.plit.FO.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    @GetMapping("/chat")
    public String chatPage(Model model, @AuthenticationPrincipal User user) {
        if (user != null) {

            UserDTO userDTO = userService.findByUserId(user.getUsername());
            model.addAttribute("nickname", userDTO);
        }
        return "fo/chat/test/chat_test"; // templates/chat_test.html 로 이동
    }

    @MessageMapping("/chat/send")
    public void sendMessage(@Payload ChatMessageDTO chatMessage) {
        Long roomId = Long.parseLong(chatMessage.getRoomId());
        Long senderId = Long.parseLong(chatMessage.getSender());

        // DB 저장
        chatService.saveMessage(roomId, senderId, chatMessage.getContent());
        // "/sub/chat/room/{roomId}" 구독자에게 메시지 전달
        messagingTemplate.convertAndSend("/sub/chat/room/" + chatMessage.getRoomId(), chatMessage);
    }
}
