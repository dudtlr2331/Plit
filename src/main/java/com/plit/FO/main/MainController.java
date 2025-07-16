package com.plit.FO.main;

import com.plit.FO.clan.dto.ClanDTO;
import com.plit.FO.clan.service.ClanService;
import com.plit.FO.user.dto.UserDTO;
import com.plit.FO.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/main") //기본 경로를 설정
public class MainController {

    @Autowired
    private UserService userService;

    @Autowired
    private ClanService clanService;

    // GET 요청을 처리하여 main.html을 반환하는 메서드
    @GetMapping
    public String mainPage(@AuthenticationPrincipal User user, Model model) {
        if (user != null) {
            UserDTO loginUser = userService.findByUserId(user.getUsername());
            model.addAttribute("loginUser", loginUser);
        }

        List<ClanDTO> topClans = clanService.getTop3ClansByMemberCount();
        model.addAttribute("topClans", topClans);

        return "fo/main/main";
    }

    @GetMapping("/home") // /main/home 다른 경로는 이렇게
    public String homePage() {
        return "main/main";
    }
}

