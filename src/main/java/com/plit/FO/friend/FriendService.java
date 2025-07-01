package com.plit.FO.friend;

import com.plit.FO.block.BlockEntity;
import com.plit.FO.block.BlockRepository;
import com.plit.FO.user.UserDTO;
import com.plit.FO.user.UserEntity;
import com.plit.FO.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FriendService {

    @Autowired
    private FriendRepository friendRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BlockRepository blockRepository;

    public FriendDTO sendFriendRequest(Integer fromUserId, Integer toUserId) {
        FriendEntity friend = FriendEntity.builder()
                .fromUserId(fromUserId)
                .toUserId(toUserId)
                .status("PENDING")
                .createdAt(LocalDateTime.now().toString())
                .build();
        FriendEntity saved = friendRepository.save(friend);
        return toDTO(saved);
    }

    // 친구 신청 목록 조회
    public List<FriendDTO> getPendingFriendRequests(Integer currentUserSeq) {
        // 현재 유저에게 온 친구 요청 중 PENDING 상태 조회
        List<FriendEntity> requests = friendRepository.findByToUserIdAndStatus(currentUserSeq, "PENDING");

        return requests.stream()
                .map(friend -> {
                    UserDTO fromUserDTO = userRepository.findById(friend.getFromUserId())
                            .map(UserEntity::toDTO)
                            .orElse(null);

                    return FriendDTO.builder()
                            .friendsNo(friend.getFriendsNo())
                            .fromUserId(friend.getFromUserId())
                            .toUserId(friend.getToUserId())
                            .status(friend.getStatus())
                            .createAt(friend.getCreatedAt())
                            .memo(friend.getMemo())
                            .user(fromUserDTO) // 닉네임 등 접근 가능
                            .build();
                })
                .filter(dto -> dto.getUser() != null)
                .toList();
    }

    public List<FriendDTO> getAcceptedFriends(Integer currentUserSeq) {
        List<FriendEntity> friends = friendRepository
                .findByStatusAndFromUserIdOrStatusAndToUserId(
                        "ACCEPTED", currentUserSeq,
                        "ACCEPTED", currentUserSeq
                );

        return friends.stream()
                .map(f -> {
                    Integer friendSeq = f.getFromUserId().equals(currentUserSeq)
                            ? f.getToUserId()
                            : f.getFromUserId();

                    return userRepository.findById(friendSeq)
                            .map(user -> {
                                FriendDTO dto = new FriendDTO();
                                dto.setUser(user.toDTO()); // 상대방의 정보
                                dto.setMemo(f.getMemo());
                                dto.setFriendsNo(f.getFriendsNo());
                                return dto;
                            }).orElse(null);
                })
                .filter(Objects::nonNull)
                .toList();
    }

    // 친구 수락
    public void acceptFriendByNo(Integer friendNo, Integer currentUserSeq) {
        UserEntity currentUser = userRepository.findById(currentUserSeq)
                .orElseThrow(() -> new RuntimeException("유저 정보를 찾을 수 없습니다."));

        FriendEntity friend = friendRepository.findById(friendNo)
                .orElseThrow(() -> new RuntimeException("친구 요청이 존재하지 않습니다."));

        if (!friend.getToUserId().equals(currentUser.getUserSeq())) {
            throw new RuntimeException("현재 유저에게 온 요청이 아닙니다.");
        }

        friend.setStatus("ACCEPTED");
        friendRepository.save(friend);
    }


    // 친구 신청 거절
    public void declineFriend(Integer friendNo, Integer currentUserSeq) {
        UserEntity currentUser = userRepository.findById(currentUserSeq)
                .orElseThrow(() -> new RuntimeException("유저 정보를 찾을 수 없습니다."));

        FriendEntity friend = friendRepository.findById(friendNo)
                .orElseThrow(() -> new RuntimeException("친구 요청이 존재하지 않습니다."));

        if (!friend.getToUserId().equals(currentUser.getUserSeq())) {
            throw new RuntimeException("현재 유저에게 온 요청이 아닙니다.");
        }

        friend.setStatus("DECLINED");
        friendRepository.save(friend);
    }


    public void acceptFriend(Integer fromUserId, String currentUserId) {
        UserEntity currentUser = userRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new RuntimeException("유저 정보를 찾을 수 없습니다."));
        Integer toUserId = currentUser.getUserSeq();

        FriendEntity friend = friendRepository
                .findByFromUserIdAndToUserIdAndStatus(fromUserId, toUserId, "PENDING")
                .orElseThrow(() -> new RuntimeException("친구 요청이 존재하지 않습니다."));

        friend.setStatus("ACCEPTED");
        friendRepository.save(friend);
    }


    private FriendDTO toDTO(FriendEntity e) {
        return FriendDTO.builder()
                .friendsNo(e.getFriendsNo())
                .fromUserId(e.getFromUserId())
                .toUserId(e.getToUserId())
                .status(e.getStatus())
                .createAt(e.getCreatedAt())
                .build();
    }

    public void updateMemo(Integer friendNo, String memo) {

        FriendEntity friend = friendRepository.findById(friendNo)
                .orElseThrow(() -> new RuntimeException("친구 정보를 찾을 수 없습니다."));
        friend.setMemo(memo);
        friendRepository.save(friend);
    }

    @Transactional
    public void blockFriend(Integer friendNo, Integer currentUserSeq) {
        FriendEntity friend = friendRepository.findById(friendNo)
                .orElseThrow(() -> new RuntimeException("친구 정보를 찾을 수 없습니다."));

        // 요청한 유저가 friend 관계의 한 쪽인지 검증
        if (!friend.getFromUserId().equals(currentUserSeq) && !friend.getToUserId().equals(currentUserSeq)) {
            throw new RuntimeException("차단 권한이 없습니다.");
        }

        // 친구 상태를 BLOCKED로 변경
        friend.setStatus("BLOCKED");
        friendRepository.save(friend);

        // 차단 대상자 추출 (내가 아닌 쪽)
        Integer blockedUserSeq = friend.getFromUserId().equals(currentUserSeq)
                ? friend.getToUserId()
                : friend.getFromUserId();

        // 이미 차단된 관계인지 확인 (중복 방지)
        boolean alreadyBlocked = blockRepository.existsByBlockerIdAndBlockedUserIdAndIsReleasedFalse(currentUserSeq, blockedUserSeq);
        if (!alreadyBlocked) {
            BlockEntity block = BlockEntity.builder()
                    .blockerId(currentUserSeq)
                    .blockedUserId(blockedUserSeq)
                    .blockedAt(LocalDateTime.now())
                    .isReleased(false)
                    .build();

            blockRepository.save(block);
        }
    }

    public void deleteFriend(Integer friendNo, Integer currentUserSeq) {
        UserEntity currentUser = userRepository.findById(currentUserSeq)
                .orElseThrow(() -> new RuntimeException("유저 정보를 찾을 수 없습니다."));

        FriendEntity friend = friendRepository.findById(friendNo)
                .orElseThrow(() -> new RuntimeException("친구 관계가 존재하지 않습니다."));

        // 내가 관련된 친구 요청이 아닐 경우 막기
        if (!friend.getFromUserId().equals(currentUser.getUserSeq()) &&
                !friend.getToUserId().equals(currentUser.getUserSeq())) {
            throw new RuntimeException("해당 친구를 삭제할 권한이 없습니다.");
        }

        // 상태만 변경
        friend.setStatus("DECLINED");
        friendRepository.save(friend);
    }
}
