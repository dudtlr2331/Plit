package com.plit.FO.block.controller;

import com.plit.FO.block.dto.BlockDTO;
import com.plit.FO.block.service.BlockService;
import com.plit.FO.user.dto.UserDTO;
import com.plit.FO.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class BlockController {

    @Autowired
    private BlockService blockService;

    @Autowired
    private UserService userService;

    @GetMapping("/mypage/blocked")
    public String block(Model model) {
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
            username = (String) oAuth2User.getAttributes().get("email"); // 또는 nickname 등
        }

        if (username == null) {
            return "redirect:/login";
        }

        UserDTO loginUser = userService.findByUserId(username);
        if (loginUser == null) {
            return "redirect:/login";
        }

        List<BlockDTO> blockList = blockService.getBlockedUsersByBlockerId(loginUser.getUserSeq());

        model.addAttribute("loginUser", loginUser);
        model.addAttribute("viewSection", "blocked");
        model.addAttribute("blockList", blockList);

        return "fo/mypage/mypage";
    }
}
