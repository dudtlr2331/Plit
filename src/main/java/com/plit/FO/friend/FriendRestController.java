package com.plit.FO.friend;

import com.plit.FO.user.UserDTO;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friends")
public class FriendRestController {

    @Autowired
    private FriendService friendService;

    @GetMapping("/requests")
    public ResponseEntity<List<FriendDTO>> getPendingRequests(HttpSession session) {
        UserDTO loginUser = (UserDTO) session.getAttribute("loginUser");

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
    public ResponseEntity<?> acceptFriend(@PathVariable Integer friendNo, HttpSession session) {
        UserDTO currentUser = (UserDTO) session.getAttribute("loginUser");
        friendService.acceptFriendByNo(friendNo, currentUser.getUserSeq());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{friendNo}/decline")
    public ResponseEntity<?> declineFriend(@PathVariable Integer friendNo, HttpSession session) {
        UserDTO currentUser = (UserDTO) session.getAttribute("loginUser");
        friendService.declineFriend(friendNo, currentUser.getUserSeq());
        return ResponseEntity.ok().build();
    }

    // 친구 차단
    @PostMapping("/{friendNo}/block")
    public ResponseEntity<?> blockFriend(@PathVariable Integer friendNo, HttpSession session) {
        UserDTO loginUser = (UserDTO) session.getAttribute("loginUser");
        friendService.blockFriend(friendNo, loginUser.getUserSeq());
        return ResponseEntity.ok().build();
    }

    // 친구 삭제
    @DeleteMapping("/{friendNo}")
    public ResponseEntity<?> deleteFriend(@PathVariable Integer friendNo, HttpSession session) {
        UserDTO currentUser = (UserDTO) session.getAttribute("loginUser");
        friendService.deleteFriend(friendNo, currentUser.getUserSeq());
        return ResponseEntity.ok().build();
    }
}
