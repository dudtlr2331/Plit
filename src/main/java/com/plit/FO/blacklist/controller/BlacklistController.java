package com.plit.FO.blacklist.controller;

import com.plit.FO.blacklist.dto.BlacklistDTO;
import com.plit.FO.blacklist.service.BlacklistService;
import com.plit.FO.user.dto.UserDTO;
import com.plit.FO.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
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
    public String getBlacklist(Model model, @AuthenticationPrincipal User user) {
        UserDTO loginUser = null;
        Integer currentUserSeq = -1;

        if (user != null) {
            loginUser = userService.findByUserId(user.getUsername());
            currentUserSeq = loginUser.getUserSeq();
            model.addAttribute("loginUser", loginUser);
        }

        List<BlacklistDTO> blacklist = blacklistService.getAllReportsWithCount(currentUserSeq);
        model.addAttribute("blacklist", blacklist);

        return "fo/blacklist/report";
    }
}