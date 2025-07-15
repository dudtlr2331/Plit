package com.plit.FO.config;

import com.plit.FO.user.dto.UserDTO;
import com.plit.FO.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributeAdvice {

    private final UserService userService;

    @ModelAttribute("loginUser")
    public UserDTO populateLoginUser() {
        Object principal = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        if (principal == null || principal.equals("anonymousUser")) {
            System.out.println("🔴 [ModelAttribute] principal is null or anonymous");
            return null;
        }

        System.out.println("🟢 [ModelAttribute] principal class: " + principal.getClass().getSimpleName());

        String userId = null;

        if (principal instanceof User springUser) {
            userId = springUser.getUsername();

        } else if (principal instanceof DefaultOAuth2User oAuth2User) {
            userId = (String) oAuth2User.getAttribute("email");
        }

        System.out.println("🟢 [ModelAttribute] 추출된 userId: " + userId);

        if (userId == null) {
            System.out.println("🔴 [ModelAttribute] userId 추출 실패");
            return null;
        }

        UserDTO loginUser = userService.findByUserId(userId);
        System.out.println("🟢 [ModelAttribute] 조회된 loginUser: " + loginUser);

        return loginUser;
    }
}
