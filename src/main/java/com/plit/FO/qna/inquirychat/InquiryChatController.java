package com.plit.FO.qna.inquirychat;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/fo/mypage/qna/chat")
public class InquiryChatController {

    private final InquiryChatService inquiryChatService;

    // 채팅 팝업 열기
    @GetMapping("/open/{roomId}")
    public String openChat(@PathVariable Long roomId, HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
//            return "redirect:/fo/login"; // 로그인 안되어 있으면 로그인으로 보내기
            userId = 1L; // 테스트용
        }

        // 채팅방 존재 확인 (없으면 생성)
        inquiryChatService.ensureRoomExists(roomId, userId);

        model.addAttribute("roomId", roomId);
        model.addAttribute("userId", userId);

        return "fo/mypage/qna/chat"; // => templates/fo/mypage/qna/chat.html
    }
}