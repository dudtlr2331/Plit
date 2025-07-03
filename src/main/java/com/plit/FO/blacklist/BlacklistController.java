package com.plit.FO.blacklist;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BlacklistController {

    @GetMapping("/report")
    public String getBlacklist(Model model, HttpSession session) {
        Object loginUser = session.getAttribute("loginUser");
        if (loginUser != null) {
            model.addAttribute("loginUser", loginUser);
        }
        return "fo/blacklist/report";
    }
}