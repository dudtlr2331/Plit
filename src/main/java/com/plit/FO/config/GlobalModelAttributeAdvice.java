package com.plit.FO.config;

import com.plit.FO.user.dto.UserDTO;
import com.plit.FO.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAttributeAdvice {

    private final UserService userService;

    @ModelAttribute("loginUser")
    public UserDTO populateLoginUser(@AuthenticationPrincipal User user) {
        if (user == null) return null;
        return userService.findByUserId(user.getUsername());
    }
}
