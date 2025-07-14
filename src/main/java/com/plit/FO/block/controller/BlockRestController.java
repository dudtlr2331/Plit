package com.plit.FO.block.controller;

import com.plit.FO.block.service.BlockService;
import com.plit.FO.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/blocks")
public class BlockRestController {

    @Autowired
    private BlockService blockService;

    @Autowired
    private UserService userService;

    @PostMapping("/{blockNo}/release")
    public ResponseEntity<?> releaseBlock(@PathVariable Integer blockNo) {
        blockService.releaseBlock(blockNo);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/direct")
    public ResponseEntity<?> directBlock(@AuthenticationPrincipal User user,
                                         @RequestBody Map<String, String> request) {
        Integer blockerId = userService.findByUserId(user.getUsername()).getUserSeq();

        // 닉네임으로 조회
        String nickname = request.get("blockedUserId"); // 실제로는 nickname
        Integer blockedUserId = userService.findByUserNickname(nickname)
                .map(u -> u.getUserSeq())
                .orElseThrow(() -> new IllegalArgumentException("해당 유저 없음"));

        blockService.blockUser(blockerId, blockedUserId);
        return ResponseEntity.ok().build();
    }



}
