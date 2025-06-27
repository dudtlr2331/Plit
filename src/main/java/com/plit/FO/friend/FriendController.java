package com.plit.FO.friend;

import com.plit.FO.user.UserDTO;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class FriendController {

    @Autowired
    private FriendService friendService;

    @GetMapping("/mypage/friends")
    public String getFriendList(Model model, HttpSession session) {
        UserDTO loginUser = (UserDTO) session.getAttribute("loginUser");

        if (loginUser == null) {
            return "redirect:/fo/login";  // 로그인 안 되어 있으면 로그인 페이지로
        }

        Integer userSeq = loginUser.getUserSeq();
        List<FriendDTO> friendList = friendService.getAcceptedFriends(userSeq);
        model.addAttribute("friendList", friendList);
        return "fo/mypage/mypage_friends";
    }
}
