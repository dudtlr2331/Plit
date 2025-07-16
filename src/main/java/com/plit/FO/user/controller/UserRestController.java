package com.plit.FO.user.controller;

import com.plit.FO.friend.service.FriendService;
import com.plit.FO.block.service.BlockService;
import com.plit.FO.user.dto.UserDTO;
import com.plit.FO.user.entity.UserEntity;
import com.plit.FO.user.repository.UserRepository;
import com.plit.FO.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserRestController {

    private final UserService userService;
    private final FriendService friendService;
    private final BlockService blockService;

    @GetMapping("/{userId}/relation-status")
    public UserDTO getRelationStatusById(@PathVariable("userId") String userId,
                                         @AuthenticationPrincipal(expression = "userSeq") Integer loginUserSeq) {
        UserDTO targetUser = userService.findByUserId(userId);
        if (targetUser == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");
        }

        boolean isFriend = false;
        boolean isBlocked = false;
        if (loginUserSeq != null) {
            isFriend = friendService.isFriend(loginUserSeq, targetUser.getUserSeq());
            isBlocked = blockService.isBlocked(loginUserSeq, targetUser.getUserSeq());
        }

        targetUser.setIsFriend(isFriend);
        targetUser.setIsBlocked(isBlocked);
        targetUser.setUserPwd(null);

        return targetUser;
    }


}
