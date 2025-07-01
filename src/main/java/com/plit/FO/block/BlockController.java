package com.plit.FO.block;

import com.plit.FO.user.UserDTO;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class BlockController {

    @Autowired
    private BlockService blockService;

    @GetMapping("/mypage/blocked")
    public String block(Model model, HttpSession session) {
        UserDTO loginUser = (UserDTO) session.getAttribute("loginUser");
        if (loginUser != null) {
            List<BlockDTO> blockList = blockService.getBlockedUsersByBlockerId(loginUser.getUserSeq());
            model.addAttribute("blockList", blockList);
        }
        return "fo/mypage/mypage_block";
    }

}
