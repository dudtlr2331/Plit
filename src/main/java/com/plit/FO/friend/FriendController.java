package com.plit.FO.friend;

import com.plit.FO.user.UserDTO;
import com.plit.FO.user.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class FriendController {

    @Autowired
    private FriendService friendService;

    @Autowired
    private UserService userService;

    @GetMapping("/mypage/friends")
    public String getFriendList(@AuthenticationPrincipal User user, Model model) {
        UserDTO loginUser = userService.findByUserId(user.getUsername());

        if (loginUser == null) {
            return "redirect:/login";  // 로그인 안 되어 있으면 로그인 페이지로
        }

        Integer userSeq = loginUser.getUserSeq();
        List<FriendDTO> friendList = friendService.getAcceptedFriends(userSeq);

        model.addAttribute("loginUser", loginUser);
        model.addAttribute("friendList", friendList);
        return "fo/mypage/mypage_friends";
    }
}
