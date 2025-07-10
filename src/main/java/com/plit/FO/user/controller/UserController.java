package com.plit.FO.user.controller;

import com.plit.FO.user.dto.UserDTO;
import com.plit.FO.user.security.CustomUserDetails;
import com.plit.FO.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 로그인 폼
    @GetMapping("/login")
    public String showLoginForm(HttpServletRequest request, Model model) {
        model.addAttribute("userDTO", new UserDTO());
        String loginError = (String) request.getSession().getAttribute("loginError");
        if (loginError != null) {
            model.addAttribute("error", loginError);
            request.getSession().removeAttribute("loginError");
        }
        return "fo/user/login";
    }

    // 회원가입 폼
    @GetMapping("/signup")
    public String showRegisterForm(Model model) {
        model.addAttribute("userDTO", new UserDTO());
        return "fo/user/signup";
    }

    // 아이디 중복확인
    @GetMapping("/check-id")
    @ResponseBody
    public Map<String, Object> checkId(@RequestParam("userId") String userId) {
        boolean available = userService.isUserIdAvailable(userId);
        return Map.of("available", available);
    }

    // 회원가입 처리
    @PostMapping("/signup")
    public String processRegistration(@ModelAttribute UserDTO userDTO,
                                      RedirectAttributes redirectAttributes) {
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

    // 비밀번호 변경
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

    // 닉네임 변경
    @PostMapping("/mypage/change-nickname")
    public String changeNickname(@AuthenticationPrincipal Object principal,
                                 @RequestParam("newNickname") String nickname,
                                 RedirectAttributes redirectAttributes) {
        String userId = null;

        if (principal instanceof CustomUserDetails userDetails) {
            userId = userDetails.getUserDTO().getUserId();
        } else if (principal instanceof DefaultOAuth2User oAuth) {
            Map<String, Object> kakaoAccount = (Map<String, Object>) oAuth.getAttributes().get("kakao_account");
            userId = (String) kakaoAccount.get("email");
            if (userId == null) userId = "kakao_" + oAuth.getName(); // fallback
        }

        if (userId != null) {
            try {
                userService.updateNickname(userId, nickname);
                redirectAttributes.addFlashAttribute("message", "닉네임이 변경되었습니다.");
            } catch (IllegalArgumentException e) {
                redirectAttributes.addFlashAttribute("error", e.getMessage());
            }
        }

        return "redirect:/mypage";
    }

    // 회원 탈퇴
    @PostMapping("/user/delete")
    public String deleteUser(@AuthenticationPrincipal Object principal,
                             HttpServletRequest request,
                             RedirectAttributes redirectAttributes) {

        Integer userSeq = null;

        if (principal instanceof CustomUserDetails userDetails) {
            userSeq = userDetails.getUserDTO().getUserSeq();
        } else if (principal instanceof OAuth2User oAuth2User) {
            try {
                Map<String, Object> kakaoAccount = (Map<String, Object>) oAuth2User.getAttributes().get("kakao_account");
                String email = (String) kakaoAccount.get("email");

                userSeq = userService.getUserByUserId(email)
                        .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."))
                        .getUserSeq();
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "사용자 정보를 찾을 수 없습니다.");
                return "redirect:/mypage";
            }
        }

        if (userSeq == null) {
            redirectAttributes.addFlashAttribute("error", "로그인 후 이용 가능합니다.");
            return "redirect:/login";
        }

        userService.deleteUser(userSeq);
        request.getSession().invalidate(); // 세션 초기화
        redirectAttributes.addFlashAttribute("message", "회원 탈퇴가 완료되었습니다.");

        return "redirect:/main";
    }



    // 로그아웃 리다이렉트
    @GetMapping("/logout")
    public String logoutRedirect() {
        return "redirect:/";
    }

    // 이메일 인증번호 전송
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

    // 이메일 인증번호 확인
    @PostMapping("/verify-code")
    @ResponseBody
    public String verifyCode(@RequestParam String email,
                             @RequestParam String inputCode) {
        boolean ok = userService.verifyEmailCode(email, inputCode);
        return ok ? "인증 성공" : "인증 실패";
    }

    @GetMapping("/mypage/main")
    public String userMainPage() {
        return "fo/user/main";
    }
}
