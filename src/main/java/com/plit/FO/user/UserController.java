package com.plit.FO.user;

import com.plit.FO.user.UserDTO;
import com.plit.FO.user.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService; // Inject UserService

    /// 로그인 버튼
    @GetMapping("/login")
    public String showLoginForm(Model model) {
        // Add a UserDTO object to the model for form binding
        model.addAttribute("userDTO", new UserDTO());
        return "fo/user/login"; // Maps to src/main/resources/templates/fo/login/login.html
    }

    /// 로그인
    // URL: /fo/login
    @PostMapping("/login")
    public String processLogin(@ModelAttribute UserDTO userDTO, RedirectAttributes redirectAttributes, HttpSession session) {
        Optional<UserDTO> loginResult = userService.loginUser(userDTO.getUserId(), userDTO.getUserPwd());
        if (loginResult.isPresent()) {
            // 로그인 성공
            UserDTO loginUser = loginResult.get();
            session.setAttribute("loginUser", loginUser); // 로그인 정보 세션에 저장

            redirectAttributes.addFlashAttribute("message", "로그인 성공!");
            return "redirect:/main";
        } else {
            // 로그인 실패
            redirectAttributes.addFlashAttribute("error", "아이디 또는 비밀번호가 일치하지 않습니다.");
            return "redirect:/login"; // 로그인 페이지로 이동
        }
    }

    /// 로그아웃
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/main";
    }


    /// 회원가입 버튼
    // URL: /fo/register
    @GetMapping("/signup")
    public String showRegisterForm(Model model) {
        model.addAttribute("userDTO", new UserDTO());
        return "fo/user/signup";
    }

    /// 회원가입 아이디 중복 검사
    @GetMapping("/check-id")
    @ResponseBody
    public Map<String, Object> checkId(@RequestParam("userId") String userId) {
        boolean available = userService.isUserIdAvailable(userId);
        return Map.of("available", available);
    }

    /// 회원가입
    // URL: /fo/signup
    @PostMapping("/signup")
    public String processRegistration(@ModelAttribute UserDTO userDTO, RedirectAttributes redirectAttributes) {
        try {
            userService.registerUser(userDTO);
            redirectAttributes.addFlashAttribute("message", "회원가입 성공! 로그인해주세요.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage()); // Display specific error (e.g., duplicate ID)
            return "redirect:/signup";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "회원가입 중 오류가 발생했습니다.");
            return "redirect:/signup";
        }
    }


    /// 회원탈퇴
    @PostMapping("/user/delete")
    public String deleteUser(@RequestParam("userSeq") Integer userSeq, HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(userSeq);
            session.invalidate();
            redirectAttributes.addFlashAttribute("message", "회원 탈퇴가 완료되었습니다.");
            return "redirect:/main";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:fo/mypage/mypage";
        }
    }

    /// 비밀번호 변경
    @PostMapping("/mypage/change-password")
    public String changePassword(@RequestParam String currentPwd,
                                 @RequestParam String newPwd,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        UserDTO loginUser = (UserDTO) session.getAttribute("loginUser");

        if (loginUser == null) {
            redirectAttributes.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/login";
        }

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
    public String sendCode(@RequestParam String email, HttpSession session) {

        String code = userService.sendEmailCode(email);

        session.setAttribute("emailCode", code);
        session.setAttribute("codeExpireTime",
                System.currentTimeMillis() + 3 * 60 * 1000);   // 3분

        return "인증번호가 전송되었습니다.";
    }


    /// 인증번호 확인
    @PostMapping("/verify-code")
    @ResponseBody
    public String verifyCode(@RequestParam String inputCode, HttpSession session) {

        Long expire = (Long) session.getAttribute("codeExpireTime");
        if (expire == null || System.currentTimeMillis() > expire) {
            return "만료된 인증번호입니다.";
        }

        String savedCode = (String) session.getAttribute("emailCode");
        boolean ok = userService.verifyEmailCode(inputCode, savedCode);

        return ok ? "인증 성공" : "인증 실패";
    }

    /// 닉네임 변경
    @PostMapping("/mypage/change-nickname")
    public String changeNickname(@RequestParam("newNickname") String nickname, HttpSession session) {
        UserDTO loginUser = (UserDTO) session.getAttribute("loginUser");
        userService.updateNickname(loginUser.getUserId(), nickname);

        loginUser.setUserNickname(nickname); // ✅ 세션 정보 갱신

        return "redirect:/mypage";
    }



}