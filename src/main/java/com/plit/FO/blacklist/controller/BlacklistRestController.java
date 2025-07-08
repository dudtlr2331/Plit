package com.plit.FO.blacklist.controller;

import com.plit.FO.blacklist.service.BlacklistService;
import com.plit.FO.blacklist.dto.BlacklistDTO;
import com.plit.FO.user.dto.UserDTO;
import com.plit.FO.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/blacklist")
@RequiredArgsConstructor
public class BlacklistRestController {

    private final BlacklistService blacklistService;
    private final UserService userService;

    @PostMapping("/report")
    public ResponseEntity<?> report(@RequestBody BlacklistDTO dto, @AuthenticationPrincipal User user) {
        UserDTO loginUser = userService.findByUserId(user.getUsername());
        if (loginUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        // 신고대상 닉네임이 존재하지 않을 경우
        try {
            blacklistService.report(dto, loginUser);
            return ResponseEntity.ok("신고가 접수되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}