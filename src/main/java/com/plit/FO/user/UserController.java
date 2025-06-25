package com.plit.FO.user;

import com.plit.FO.user.UserDTO;
import com.plit.FO.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/fo") // All requests to this controller start with /fo
@RequiredArgsConstructor
public class UserController {

    private final UserService userService; // Inject UserService

    // Handles the main page for Front Office
    // URL: /fo/main
    @GetMapping("/main")
    public String mainPage() {
        return "fo/main/main"; // Maps to src/main/resources/templates/fo/main/main.html
    }

    // Displays the login form
    // URL: /fo/login
    @GetMapping("/login")
    public String showLoginForm(Model model) {
        // Add a UserDTO object to the model for form binding
        model.addAttribute("userDTO", new UserDTO());
        return "fo/login/login"; // Maps to src/main/resources/templates/fo/login/login.html
    }

    // Processes the login form submission
    // URL: /fo/login
    @PostMapping("/login")
    public String processLogin(@ModelAttribute UserDTO userDTO, RedirectAttributes redirectAttributes) {
        // Here, you would typically use Spring Security or similar for proper authentication.
        // For this example, we're doing a simple check with UserService.
        if (userService.loginUser(userDTO.getUserId(), userDTO.getUserPwd()).isPresent()) {
            // Login successful
            // You might add user info to session here
            redirectAttributes.addFlashAttribute("message", "로그인 성공!");
            return "redirect:/fo/main"; // Redirect to FO main page on success
        } else {
            // Login failed
            redirectAttributes.addFlashAttribute("error", "아이디 또는 비밀번호가 일치하지 않습니다.");
            return "redirect:/fo/login"; // Redirect back to login page on failure
        }
    }

    // Displays the registration form
    // URL: /fo/register
    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("userDTO", new UserDTO());
        // 에러 로그에 따라 'fo/login/register.html'로 경로 수정
        return "fo/login/register"; // <-- 이 부분을 'fo/user/register' 에서 변경했습니다.
    }

    // Processes the registration form submission
    // URL: /fo/register
    @PostMapping("/register")
    public String processRegistration(@ModelAttribute UserDTO userDTO, RedirectAttributes redirectAttributes) {
        try {
            userService.registerUser(userDTO);
            redirectAttributes.addFlashAttribute("message", "회원가입 성공! 로그인해주세요.");
            return "redirect:/fo/login"; // Redirect to login page after successful registration
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage()); // Display specific error (e.g., duplicate ID)
            return "redirect:/fo/register"; // Redirect back to register page on failure
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "회원가입 중 오류가 발생했습니다.");
            return "redirect:/fo/register";
        }
    }
}