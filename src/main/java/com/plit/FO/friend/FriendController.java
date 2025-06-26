package com.plit.FO.friend;

import com.plit.FO.user.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.List;

@Controller
public class FriendController {

    @Autowired
    private FriendService friendService;


    @GetMapping("/mypage/friends")
    public String getFriendList(Model model, Principal principal) {
        String currentUserId = principal.getName(); // 로그인된 userId
        List<UserDTO> friendList = friendService.getAcceptedFriends(currentUserId);
        model.addAttribute("friendList", friendList);
        return "mypage";
    }
}
