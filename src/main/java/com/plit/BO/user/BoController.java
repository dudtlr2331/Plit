package com.plit.BO.user;

import com.plit.FO.user.UserDTO;
import com.plit.FO.user.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class BoController {
    @Autowired
    private UserService userService;

    @GetMapping("/bo")
    public String boIndex() {
        return "bo/admin/index";
    }
    @GetMapping("/index")
    public String index(HttpSession session, Model model) {
        UserDTO loginUser = (UserDTO) session.getAttribute("loginUser");

        if (loginUser == null) {
            return "redirect:/login"; // 로그인 안 됨
        }

        String auth = loginUser.getUserAuth();
        if (!auth.equals("admin") && !auth.equals("master")) {
            return "redirect:/main"; // 권한 없음
        }

        // 🔹 관리자 목록 추가
        List<UserDTO> adminList = userService.getAllUsers().stream()
                .filter(user -> "admin".equals(user.getUserAuth()) || "master".equals(user.getUserAuth()))
                .collect(Collectors.toList());

        model.addAttribute("adminList", adminList); // 🔹 Thymeleaf에 전달
        return "bo/admin/index";
    }

    @GetMapping("/bo/manage_user")
    public String manageUser(@RequestParam(required = false) String keyword, Model model) {
        List<UserDTO> userList;

        if (keyword != null && !keyword.isBlank()) {
            userList = userService.searchByNickname(keyword);
        } else {
            userList = userService.getAllUsers();
        }

        model.addAttribute("userList", userList);
        model.addAttribute("keyword", keyword);
        return "bo/admin/manage_user";
    }

    @GetMapping("/layout-static")
    public String layoutStatic() {
        return "bo/test/layout-static";
    }

    @GetMapping("/layout-sidenav-light")
    public String layoutSidenavLight() {
        return "bo/test/layout-sidenav-light";
    }

    @GetMapping("/bo/login")
    public String boLogin() {
        return "bo/test/login";
    }

    @GetMapping("/bo/register")
    public String boRegister() {
        return "bo/test/register";
    }

    @GetMapping("/bo/password")
    public String boPassword() {
        return "bo/test/password";
    }

    @GetMapping("/401")
    public String bo401() {
        return "bo/test/401";
    }

    @GetMapping("/404")
    public String bo404() {
        return "bo/test/404";
    }

    @GetMapping("/500")
    public String bo500() {
        return "bo/test/500";
    }

    @GetMapping("/charts")
    public String charts() {
        return "bo/test/charts";
    }

    @GetMapping("/tables")
    public String tables() {
        return "bo/test/tables";
    }

}
