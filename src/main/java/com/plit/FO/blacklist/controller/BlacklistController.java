package com.plit.FO.blacklist.controller;

import com.plit.FO.blacklist.dto.BlacklistDTO;
import com.plit.FO.blacklist.service.BlacklistService;
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
public class BlacklistController {
    @Autowired
    private BlacklistService blacklistService;

    @Autowired
    private UserService userService;

    @GetMapping("/report")
    public String getBlacklist(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        UserDTO loginUser = null;
        if (authentication != null && authentication.isAuthenticated()
                && !authentication.getPrincipal().equals("anonymousUser")) {

            Object principal = authentication.getPrincipal();
            String username = null;

            if (principal instanceof UserDetails userDetails) {
                username = userDetails.getUsername();
            } else if (principal instanceof OAuth2User oAuth2User) {
                username = (String) oAuth2User.getAttributes().get("email");
            }

            if (username != null) {
                loginUser = userService.findByUserId(username);
            }
        }

        Integer currentUserSeq = (loginUser != null) ? loginUser.getUserSeq() : -1;

        List<BlacklistDTO> blacklist = blacklistService.getAllReportsWithCount(currentUserSeq);

        model.addAttribute("loginUser", loginUser);
        model.addAttribute("blacklist", blacklist);

        return "fo/blacklist/report";
    }

}