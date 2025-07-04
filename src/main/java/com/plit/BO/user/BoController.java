package com.plit.BO.user;

import com.plit.FO.blacklist.dto.BlacklistDTO;
import com.plit.FO.blacklist.service.BlacklistService;
import com.plit.FO.user.UserDTO;
import com.plit.FO.user.UserRepository;
import com.plit.FO.user.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
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

    @Autowired
    private BlacklistService blacklistService;
    @Autowired
    private UserRepository userRepository;

    @GetMapping("/bo")
    public String boIndex() {
        return "bo/admin/index";
    }
    @GetMapping("/index")
    public String index(@AuthenticationPrincipal User user, Model model) {
        UserDTO loginUser = userService.findByUserId(user.getUsername());

        if (loginUser == null) {
            return "redirect:/login"; // 로그인 안 됨
        }

        String auth = loginUser.getUserAuth();
        if (!auth.equals("admin") && !auth.equals("master")) {
            return "redirect:/main"; // 권한 없음
        }

        // 🔹 관리자 목록 추가
        List<UserDTO> adminList = userService.getAllUsers().stream()
                .filter(u -> "admin".equals(u.getUserAuth()) || "master".equals(u.getUserAuth()))
                .collect(Collectors.toList());

        model.addAttribute("loginUser", loginUser);
        model.addAttribute("adminList", adminList); // 🔹 Thymeleaf에 전달
        return "bo/admin/index";
    }

    @GetMapping("/bo/personal_qna")
    public String personalQna(HttpSession session, Model model) {
        return "bo/admin/personal_qna";
    }

    @GetMapping("bo/unprocessed_qna")
    public String unprocessedQna(HttpSession session, Model model) {
        return "bo/admin/unprocessed_qna";
    }
    @GetMapping("bo/trol")
    public String trol(@AuthenticationPrincipal User user,
                       @RequestParam(required = false, defaultValue = "ALL") String status,
                       @RequestParam(required = false) String keyword,
                       Model model) {

        UserDTO loginUser = userService.findByUserId(user.getUsername());
        Integer currentUserSeq = (loginUser != null) ? loginUser.getUserSeq() : -1;

        List<BlacklistDTO> allReports = blacklistService.getAllReportsWithCount(currentUserSeq);

        // 키워드 필터링
        if (keyword != null && !keyword.isBlank()) {
            allReports = allReports.stream()
                    .filter(dto ->
                            dto.getReporterNickname().toLowerCase().contains(keyword.toLowerCase()) ||
                                    dto.getReportedNickname().toLowerCase().contains(keyword.toLowerCase()) ||
                                    dto.getReason().toLowerCase().contains(keyword.toLowerCase()))
                    .collect(Collectors.toList());
        }

        // 상태 필터링
        List<BlacklistDTO> filteredReports = allReports.stream()
                .filter(dto -> {
                    if ("ALL".equals(status)) return true;
                    return status.equals(dto.getStatus());
                })
                .collect(Collectors.toList());

        model.addAttribute("loginUser", loginUser);
        model.addAttribute("blacklistList", filteredReports);
        model.addAttribute("status", status);
        model.addAttribute("keyword", keyword);
        return "bo/admin/trol";
    }

    @GetMapping("/bo/manage_user")
    public String manageUser(@AuthenticationPrincipal User user,
                             @RequestParam(required = false) String keyword,
                             @RequestParam(required = false, defaultValue = "ALL") String status,
                             Model model) {
        UserDTO loginUser = userService.findByUserId(user.getUsername());
        List<UserDTO> allUsers;

        // 1. 키워드가 존재하면 닉네임으로 검색
        if (keyword != null && !keyword.isBlank()) {
            allUsers = userService.searchByNickname(keyword);
        } else {
            allUsers = userService.getAllUsers();
        }

        // 2. 상태 필터링
        List<UserDTO> filteredUsers = allUsers.stream()
                .filter(u -> {
                    return switch (status) {
                        case "NORMAL" -> "Y".equals(u.getUseYn()) && !Boolean.TRUE.equals(u.getIsBanned());
                        case "BLOCKED" -> Boolean.TRUE.equals(u.getIsBanned());
                        case "INACTIVE" -> "N".equals(u.getUseYn());
                        case "ALL" -> true;
                        default -> true;
                    };
                })
                .collect(Collectors.toList());

        // 3. 모델에 데이터 전달
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("userList", filteredUsers);
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);

        return "bo/admin/manage_user";
    }



    @GetMapping("/401")
    public String bo401() {
        return "bo/admin/401";
    }

    @GetMapping("/404")
    public String bo404() {
        return "bo/admin/404";
    }

    @GetMapping("/500")
    public String bo500() {
        return "bo/admin/500";
    }


}
