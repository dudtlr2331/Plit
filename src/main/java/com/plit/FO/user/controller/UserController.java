package com.plit.FO.user.controller;

import com.plit.FO.user.security.CustomUserDetails;
import com.plit.FO.user.dto.UserDTO;
import com.plit.FO.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService; // Inject UserService

    /// 로그인 폼
    @GetMapping("/login")
    public String showLoginForm(HttpServletRequest request, Model model) {
        model.addAttribute("userDTO", new UserDTO());

        // 로그인 실패 메시지 처리
        String loginError = (String) request.getSession().getAttribute("loginError");
        if (loginError != null) {
            model.addAttribute("error", loginError);
            request.getSession().removeAttribute("loginError");
        }

        return "fo/user/login";
    }

    /// 회원가입 폼
    @GetMapping("/signup")
    public String showRegisterForm(Model model) {
        model.addAttribute("userDTO", new UserDTO());
        return "fo/user/signup";
    }

    /// 아이디 중복확인
    @GetMapping("/check-id")
    @ResponseBody
    public Map<String, Object> checkId(@RequestParam("userId") String userId) {
        boolean available = userService.isUserIdAvailable(userId);
        return Map.of("available", available);
    }

    /// 회원가입
    @PostMapping("/signup")
    public String processRegistration(@ModelAttribute UserDTO userDTO, RedirectAttributes redirectAttributes) {
        try {
            userService.registerUser(userDTO);
            redirectAttributes.addFlashAttribute("message", "회원가입 성공! 로그인해주세요.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/signup";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "회원가입 중 오류가 발생했습니다.");
            return "redirect:/signup";
        }
    }

    /// 회원 탈퇴
    @PostMapping("/user/delete")
    public String deleteUser(@AuthenticationPrincipal CustomUserDetails userDetails,
                             RedirectAttributes redirectAttributes,
                             HttpServletRequest request,
                             HttpServletResponse response) {
        try {
            Integer userSeq = userDetails.getUserDTO().getUserSeq();
            userService.deleteUser(userSeq);

            // Spring Security 로그아웃 처리
            SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
            logoutHandler.logout(request, response, null);

            redirectAttributes.addFlashAttribute("message", "회원 탈퇴가 완료되었습니다.");
            return "redirect:/main";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/mypage";
        }
    }

    /// 로그아웃
    @GetMapping("/logout")
    public String logoutRedirect() {
        return "redirect:/";
    }

    /// 비밀번호 변경
    @PostMapping("/mypage/change-password")
    public String changePassword(@AuthenticationPrincipal CustomUserDetails userDetails,
                                 @RequestParam String currentPwd,
                                 @RequestParam String newPwd,
                                 RedirectAttributes redirectAttributes) {

        UserDTO loginUser = userDetails.getUserDTO();

        boolean isChanged = userService.changePassword(loginUser.getUserId(), currentPwd, newPwd);

        if (isChanged) {
            redirectAttributes.addFlashAttribute("message", "비밀번호가 변경되었습니다.");
        } else {
            redirectAttributes.addFlashAttribute("error", "현재 비밀번호가 올바르지 않습니다.");
        }

        return "redirect:/mypage";
    }

    /// 인증번호 전송
    @PostMapping("/send-code")
    @ResponseBody
    public ResponseEntity<String> sendCode(@RequestParam String email) {
        try {
            userService.sendEmailCode(email);
            return ResponseEntity.ok("인증번호가 전송되었습니다.");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /// 인증번호 확인
    @PostMapping("/verify-code")
    @ResponseBody
    public String verifyCode(@RequestParam String email,
                             @RequestParam String inputCode) {

        boolean ok = userService.verifyEmailCode(email, inputCode);
        return ok ? "인증 성공" : "인증 실패";
    }

    /// 닉네임 변경
    @PostMapping("/mypage/change-nickname")
    public String changeNickname(@AuthenticationPrincipal CustomUserDetails userDetails,
                                 @RequestParam("newNickname") String nickname) {
        UserDTO loginUser = userDetails.getUserDTO();
        userService.updateNickname(loginUser.getUserId(), nickname);

        return "redirect:/mypage";
    }
}