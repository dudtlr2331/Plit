package com.plit.FO.block.controller;

import com.plit.FO.block.dto.BlockDTO;
import com.plit.FO.block.service.BlockService;
import com.plit.FO.user.UserDTO;
import com.plit.FO.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
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
    public String block(Model model, @AuthenticationPrincipal User user) {
        UserDTO loginUser = userService.findByUserId(user.getUsername());
        if (loginUser != null) {
            List<BlockDTO> blockList = blockService.getBlockedUsersByBlockerId(loginUser.getUserSeq());
            model.addAttribute("blockList", blockList);
        }
        return "fo/mypage/mypage_block";
    }

}
