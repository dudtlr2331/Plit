package com.plit.FO.friend.controller;

import com.plit.FO.friend.dto.FriendDTO;
import com.plit.FO.friend.service.FriendService;
import com.plit.FO.user.dto.UserDTO;
import com.plit.FO.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
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
    public String getFriendList(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal().equals("anonymousUser")) {
            return "redirect:/login";
        }

        Object principal = authentication.getPrincipal();
        String username = null;

        if (principal instanceof UserDetails userDetails) {
            username = userDetails.getUsername();
        } else if (principal instanceof OAuth2User oAuth2User) {
            // 카카오 로그인일 경우 email 또는 nickname을 기준으로 user 조회
            username = (String) oAuth2User.getAttributes().get("email"); // email이 DB userId라면
            // username = (String) oAuth2User.getAttributes().get("nickname"); // nickname을 기준으로 할 경우
        }

        if (username == null) {
            return "redirect:/login";
        }

        UserDTO loginUser = userService.findByUserId(username);
        if (loginUser == null) {
            return "redirect:/login";
        }

        Integer userSeq = loginUser.getUserSeq();
        List<FriendDTO> friendList = friendService.getAcceptedFriends(userSeq);

        model.addAttribute("viewSection", "friends");
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("friendList", friendList);
        return "fo/mypage/mypage";
    }

}
