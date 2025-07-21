package com.plit.BO.user;

import com.plit.FO.blacklist.dto.BlacklistDTO;
import com.plit.FO.blacklist.entity.BlacklistEntity;
import com.plit.FO.blacklist.repository.BlacklistRepository;
import com.plit.FO.blacklist.service.BlacklistService;
import com.plit.FO.user.dto.UserDTO;
import com.plit.FO.user.entity.UserEntity;
import com.plit.FO.user.repository.UserRepository;
import com.plit.FO.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bo/admin")
@RequiredArgsConstructor
public class BoRestController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final BlacklistService blacklistService;
    private final BlacklistRepository blacklistRepository;


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
    public String updateAdminByForm(@PathVariable Integer userSeq, @RequestParam String userNickname, @RequestParam String userAuth) {
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

        // 변경된 상태를 클라이언트에 반환하거나, 성공 상태만 확실히 반환
        return ResponseEntity.ok().build();
    }


    @PutMapping("/user/update/{userSeq}")
    public ResponseEntity<Void> updateUserInfo(@PathVariable Integer userSeq, @RequestBody Map<String, String> payload) {
        String nickname = payload.get("userNickname");
        String auth = payload.get("userAuth");

        userService.updateUserInfo(userSeq, nickname, auth);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/report/{blacklistNo}/{action}")
    public ResponseEntity<Void> handleReportStatus(@PathVariable Integer blacklistNo, @PathVariable String action, @AuthenticationPrincipal User user) {
        UserDTO loginUser = userService.findByUserId(user.getUsername());
        if (loginUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        String newStatus = action.equals("ACCEPTED") ? "ACCEPTED" : "DECLINED";
        blacklistService.updateReportStatus(blacklistNo, newStatus, loginUser.getUserSeq());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/report/history/{reportedUserId}")
    public List<BlacklistDTO> getReportHistory(@PathVariable Integer reportedUserId) {
        List<BlacklistEntity> list = blacklistRepository.findByReportedUserId(reportedUserId);

        return list.stream().map(entity -> {
            BlacklistDTO dto = new BlacklistDTO();
            dto.setReason(entity.getReason());
            dto.setReportedAt(entity.getReportedAt());

            String reporterNickname = userRepository.findById(entity.getReporterId())
                    .map(UserEntity::getUserNickname)
                    .orElse("알 수 없음");
            dto.setReporterNickname(reporterNickname);

            return dto;
        }).collect(Collectors.toList());
    }

    // 일괄처리 - 유저 관리
    @PutMapping("/user/bulk-status")
    public ResponseEntity<?> updateUserStatusBulk(@RequestBody BoUserDTO request) {
        List<Integer> userSeqList = request.getUserSeqList();
        String action = request.getAction();

        if (userSeqList == null || action == null || userSeqList.isEmpty()) {
            return ResponseEntity.badRequest().body("유효하지 않은 요청입니다.");
        }

        for (Integer userSeq : userSeqList) {
            userService.updateUserStatus(userSeq, action);
        }

        return ResponseEntity.ok().build();
    }

    // 일괄처리 - 트롤 신고 관리
    @PostMapping("/report/bulk")
    public ResponseEntity<?> handleBulkReportStatus(@RequestBody Map<String, Object> payload,
                                                    @AuthenticationPrincipal User user) {
        // 로그인 확인
        UserDTO loginUser = userService.findByUserId(user.getUsername());
        if (loginUser == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        // payload에서 데이터 추출
        List<Integer> blacklistNoList = ((List<?>) payload.get("blacklistNoList")).stream()
                .map(obj -> Integer.parseInt(obj.toString()))
                .toList();

        String action = (String) payload.get("action");
        if (!action.equals("ACCEPTED") && !action.equals("DECLINED")) {
            return ResponseEntity.badRequest().body("잘못된 처리 상태입니다.");
        }

        // 처리
        for (Integer blacklistNo : blacklistNoList) {
            blacklistService.updateReportStatus(blacklistNo, action, loginUser.getUserSeq());
        }

        return ResponseEntity.ok().build();
    }

}
