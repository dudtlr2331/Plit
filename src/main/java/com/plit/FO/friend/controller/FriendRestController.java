package com.plit.FO.friend.controller;

import com.plit.FO.friend.dto.FriendDTO;
import com.plit.FO.friend.service.FriendService;
import com.plit.FO.user.dto.UserDTO;
import com.plit.FO.user.entity.UserEntity;
import com.plit.FO.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/friends")
public class FriendRestController {

    @Autowired
    private FriendService friendService;
    @Autowired
    private UserService userService;

    @GetMapping("/requests")
    public ResponseEntity<List<FriendDTO>> getPendingRequests(@AuthenticationPrincipal User user) {
        UserDTO loginUser = userService.findByUserId(user.getUsername());

        if (loginUser == null) {
            return ResponseEntity.badRequest().build();
        }

        List<FriendDTO> requests = friendService.getPendingFriendRequests(loginUser.getUserSeq());
        return ResponseEntity.ok(requests);
    }

    @PostMapping("/{friendNo}/memo")
    public ResponseEntity<?> updateMemo(@PathVariable Integer friendNo, @RequestBody Map<String, String> request) {
        String memo = request.get("memo");
        friendService.updateMemo(friendNo, memo);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{friendNo}/accept")
    public ResponseEntity<?> acceptFriend(@PathVariable Integer friendNo, @AuthenticationPrincipal User user) {
        UserDTO currentUser = userService.findByUserId(user.getUsername());
        friendService.acceptFriendByNo(friendNo, currentUser.getUserSeq());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{friendNo}/decline")
    public ResponseEntity<?> declineFriend(@PathVariable Integer friendNo, @AuthenticationPrincipal User user) {
        UserDTO currentUser = userService.findByUserId(user.getUsername());
        friendService.declineFriend(friendNo, currentUser.getUserSeq());
        return ResponseEntity.ok().build();
    }

    // 친구 차단
    @PostMapping("/{friendNo}/block")
    public ResponseEntity<?> blockFriend(@PathVariable Integer friendNo, @AuthenticationPrincipal User user) {
        UserDTO loginUser = userService.findByUserId(user.getUsername());
        friendService.blockFriend(friendNo, loginUser.getUserSeq());
        return ResponseEntity.ok().build();
    }

    // 친구 삭제
    @DeleteMapping("/{friendNo}")
    public ResponseEntity<?> deleteFriend(@PathVariable Integer friendNo, @AuthenticationPrincipal User user) {
        UserDTO currentUser = userService.findByUserId(user.getUsername());
        friendService.deleteFriend(friendNo, currentUser.getUserSeq());
        return ResponseEntity.ok().build();
    }

    // 친구 신청
    @PostMapping("/request")
    public ResponseEntity<?> sendFriendRequest(@AuthenticationPrincipal User user,
                                               @RequestBody Map<String, String> request) {
        UserDTO loginUser = userService.findByUserId(user.getUsername());
        if (loginUser == null) return ResponseEntity.badRequest().build();

        String userId = request.get("toUserId"); // 닉네임 기반
        UserDTO toUserDTO = userService.findByUserId(userId);

        if (toUserDTO == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("해당 닉네임을 가진 사용자가 존재하지 않습니다.");
        }

        Integer toUserSeq = toUserDTO.getUserSeq();
        String memo = request.getOrDefault("memo", null);

        friendService.sendFriendRequest(loginUser.getUserSeq(), toUserSeq, memo);
        return ResponseEntity.ok().build();
    }

    // 친구 여부 확인
    @GetMapping("/check")
    public ResponseEntity<Boolean> isFriend(
            @AuthenticationPrincipal User user,
            @RequestParam("targetUserId") String targetUserNickname) {

        UserDTO me = userService.findByUserId(user.getUsername());
        UserDTO target = userService.findByUserId(targetUserNickname);
        if (me == null || target == null) return ResponseEntity.ok(false);

        boolean result = friendService.isFriend(me.getUserSeq(), target.getUserSeq());
        return ResponseEntity.ok(result);
    }

}
