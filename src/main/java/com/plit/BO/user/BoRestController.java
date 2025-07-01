package com.plit.BO.user;

import com.plit.FO.user.UserDTO;
import com.plit.FO.user.UserEntity;
import com.plit.FO.user.UserRepository;
import com.plit.FO.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/bo/admin")
@RequiredArgsConstructor
public class BoRestController {

    private final UserService userService;
    private final UserRepository userRepository;

    @PostMapping("/add")
    public ResponseEntity<?> addAdminAccount(@RequestBody UserDTO userDTO) {
        try {
            userDTO.setUserAuth("admin");
            userDTO.setUseYn("Y");
            userDTO.setIsBanned(false);
            userDTO.setUserCreateDate(LocalDate.now());

            userService.registerUser(userDTO);
            return ResponseEntity.ok("관리자 계정이 성공적으로 추가되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/update/{userSeq}")
    public ResponseEntity<?> updateAdminInfo(@PathVariable Integer userSeq, @RequestBody UserDTO userDTO) {
        try {
            UserDTO updated = userService.updateUser(userSeq, userDTO);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/delete/{userSeq}")
    public ResponseEntity<?> deleteAdmin(@PathVariable Integer userSeq) {
        try {
            userService.deleteUser(userSeq);
            return ResponseEntity.ok("삭제 완료");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/update/{userSeq}")
    public String updateAdminByForm(@PathVariable Integer userSeq,
                                    @RequestParam String userNickname,
                                    @RequestParam String userAuth) {
        UserDTO dto = new UserDTO();
        dto.setUserNickname(userNickname);
        dto.setUserAuth(userAuth);
        userService.updateUser(userSeq, dto);
        return "redirect:/index"; // 혹은 관리자 계정 관리 페이지 경로
    }

    @PutMapping("/user/status/{userSeq}")
    public ResponseEntity<?> updateUserStatus(@PathVariable Integer userSeq, @RequestBody Map<String, String> payload) {
        String action = payload.get("action");

        Optional<UserEntity> userOpt = userRepository.findById(userSeq);
        if (userOpt.isEmpty()) return ResponseEntity.notFound().build();

        UserEntity user = userOpt.get();

        switch (action) {
            case "BLOCK" -> user.setIsBanned(true);
            case "UNBLOCK" -> user.setIsBanned(false);
            case "DEACTIVATE" -> user.setUseYn("N");
            case "ACTIVATE" -> user.setUseYn("Y");
            default -> {
                return ResponseEntity.badRequest().body("잘못된 요청입니다.");
            }
        }

        userRepository.save(user);

        // ✅ 해결 핵심: 변경된 상태를 클라이언트에 반환하거나, 성공 상태만 확실히 반환
        return ResponseEntity.ok().build(); // 또는 상태 메시지 포함
    }


    @PutMapping("/user/update/{userSeq}")
    public ResponseEntity<Void> updateUserInfo(@PathVariable Integer userSeq, @RequestBody Map<String, String> payload) {
        String nickname = payload.get("userNickname");
        String auth = payload.get("userAuth");

        userService.updateUserInfo(userSeq, nickname, auth);
        return ResponseEntity.ok().build();
    }



}
