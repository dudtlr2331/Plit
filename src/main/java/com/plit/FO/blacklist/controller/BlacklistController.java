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
        Object loginUser = userService.findByUserId(user.getUsername());
        if (loginUser != null) {
            model.addAttribute("loginUser", loginUser);
        }

        Integer currentUserSeq = (loginUser != null) ? ((UserDTO) loginUser).getUserSeq() : -1;
        List<BlacklistDTO> blacklist = blacklistService.getAllReportsWithCount(currentUserSeq);
        model.addAttribute("blacklist", blacklist);
        model.addAttribute("loginUser", loginUser);

        return "fo/blacklist/report";
    }
}