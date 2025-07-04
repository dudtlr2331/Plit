package com.plit.FO.friend;

import com.plit.FO.user.UserDTO;
import com.plit.FO.user.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
}
