package com.plit.FO.blacklist;

import com.plit.FO.user.UserDTO;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class BlacklistController {
    @Autowired
    private BlacklistService blacklistService;

    @GetMapping("/report")
    public String getBlacklist(Model model, HttpSession session) {
        Object loginUser = session.getAttribute("loginUser");
        if (loginUser != null) {
            model.addAttribute("loginUser", loginUser);
        }

        Integer currentUserSeq = (loginUser != null) ? ((UserDTO) loginUser).getUserSeq() : -1;
        List<BlacklistDTO> blacklist = blacklistService.getAllReportsWithCount(currentUserSeq);
        model.addAttribute("blacklist", blacklist);

        return "fo/blacklist/report";
    }
}