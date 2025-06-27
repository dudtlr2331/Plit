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
@RequestMapping("/fo") // All requests to this controller start with /fo
@RequiredArgsConstructor
public class UserController {

    private final UserService userService; // Inject UserService


    /// 메인
    // URL: /fo/main
    @GetMapping("/main")
    public String mainPage() {
        return "fo/main/main"; // Maps to src/main/resources/templates/fo/main/main.html
    }

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
            return "redirect:/fo/login"; // 로그인 페이지로 이동
        }
    }

    /// 로그아웃
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/fo/main";
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
            return "redirect:/fo/login"; // Redirect to login page after successful registration
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage()); // Display specific error (e.g., duplicate ID)
            return "redirect:/fo/user/signup"; // Redirect back to register page on failure
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "회원가입 중 오류가 발생했습니다.");
            return "redirect:/fo/user/signup";
        }
    }
}