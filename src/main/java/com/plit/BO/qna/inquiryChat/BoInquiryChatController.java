package com.plit.BO.qna.inquiryChat;

import com.plit.FO.qna.inquirychat.InquiryMessage;
import com.plit.FO.qna.inquirychat.InquiryRoom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/bo/qna/chat")
public class BoInquiryChatController {

    private final BoInquiryChatService chatService;

    // 미응답 채팅방 리스트
    @GetMapping("/pending")
    @ResponseBody
    public List<InquiryRoom> getPendingRooms() {
        return chatService.getPendingRooms();
    }

    // 채팅 팝업 열기
    @GetMapping("/open/{roomId}")
    public String openChat(@PathVariable Long roomId, HttpSession session, Model model) {
        Long adminId = (Long) session.getAttribute("adminId");
        if (adminId == null) {

//            return "redirect:/bo/login"; // 로그인 안되어 있으면 로그인 페이지로
            adminId = 1L; //테스트용

        }

        // 채팅방에 관리자 배정
        chatService.assignAdminToRoom(roomId, adminId);

        model.addAttribute("roomId", roomId);
        model.addAttribute("adminId", adminId);
        return "bo/qna/bocaht"; // => templates/bo/qna/bocaht.html
    }

    // 메시지 조회 (필요 시 REST로 분리 가능)
    @GetMapping("/messages/{roomId}")
    @ResponseBody
    public List<InquiryMessage> getMessages(@PathVariable Long roomId) {
        return chatService.getMessages(roomId);
    }
}