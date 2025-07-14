package com.plit.FO.config;

import com.plit.FO.user.dto.UserDTO;
import com.plit.FO.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Map;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributeAdvice {

    private final UserService userService;

    @ModelAttribute("loginUser")
    public UserDTO populateLoginUser(@AuthenticationPrincipal Object principal) {
        if (principal == null) return null;

        String userId = null;

        if (principal instanceof User user) {
            userId = user.getUsername();
        } else if (principal instanceof DefaultOAuth2User oAuth2User) {
            Object kakaoAccountObj = oAuth2User.getAttribute("kakao_account");
            if (kakaoAccountObj instanceof Map<?, ?> kakaoAccount) {
                userId = (String) kakaoAccount.get("email");
            }
        }

        if (userId == null) return null;

        return userService.findByUserId(userId);
    }
}
