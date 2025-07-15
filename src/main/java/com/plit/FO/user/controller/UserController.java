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

        // 기존 로그인 에러 메시지 처리
        String loginError = (String) request.getSession().getAttribute("loginError");
        if (loginError != null) {
            model.addAttribute("error", loginError);
            request.getSession().removeAttribute("loginError");
        }

        // ✅ 재설정 성공 메시지 표시
        return "fo/user/login"; // Thymeleaf에서 th:if="${message}" 처리
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

        // 1. 새 비밀번호 형식 유효성 검증
        if (!userService.isValidPassword(newPwd, loginUser.getUserId())) {
            redirectAttributes.addFlashAttribute("error", "비밀번호는 8자 이상이며, 영문/숫자/기호 중 2가지 이상 조합이어야 하며 이메일을 포함할 수 없습니다.");
            return "redirect:/mypage";
        }

        // 2. 기존 비밀번호와 같은지 확인
        if (userService.checkPassword(loginUser.getUserId(), newPwd)) {
            redirectAttributes.addFlashAttribute("error", "기존 비밀번호와 동일한 비밀번호는 사용할 수 없습니다.");
            return "redirect:/mypage";
        }

        // 3. 비밀번호 변경 시도
        boolean isChanged = userService.changePassword(loginUser.getUserId(), currentPwd, newPwd);

        if (isChanged) {
            redirectAttributes.addFlashAttribute("message", "비밀번호가 변경되었습니다.");
        } else {
            redirectAttributes.addFlashAttribute("error", "현재 비밀번호가 올바르지 않습니다.");
        }

        return "redirect:/mypage";
    }

    // 비밀번호 찾기
    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "fo/user/forgot_password";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordPage() {
        return "fo/mypage/reset_password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam String email,
                                       @RequestParam String newPwd,
                                       RedirectAttributes redirectAttributes,
                                       Model model) {
        if (!userService.isValidPassword(newPwd, email)) {
            model.addAttribute("email", email);
            model.addAttribute("error", "비밀번호 규칙에 맞지 않습니다.");
            return "fo/mypage/reset_password";
        }

        userService.resetPassword(email, newPwd);
        redirectAttributes.addFlashAttribute("message", "비밀번호가 재설정되었습니다.");
        return "redirect:/login";
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
            System.out.println("이메일 인증 실패 사유: " + e.getMessage()); // 로그 찍기
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
