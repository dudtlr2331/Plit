package com.plit.FO.friend;

import com.plit.FO.user.UserDTO;
import com.plit.FO.user.UserEntity;
import com.plit.FO.user.UserRepository;
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
    public List<FriendDTO> getPendingFriendRequests(String currentUserId) {
        // 현재 userId 로 userSeq 조회
        UserEntity currentUser = userRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new RuntimeException("유저 정보를 찾을 수 없습니다."));
        Integer toUserId = currentUser.getUserSeq();

        // 해당 유저에게 온 친구 요청 중 PENDING 상태인 것들 조회
        List<FriendEntity> requests = friendRepository.findByToUserIdAndStatus(toUserId, "PENDING");

        // 결과가 없으면 빈 리스트 반환
        return requests.stream()
                .map(friend -> FriendDTO.builder()
                        .friendsNo(friend.getFriendsNo())
                        .fromUserId(friend.getFromUserId())
                        .toUserId(friend.getToUserId())
                        .status(friend.getStatus())
                        .createAt(friend.getCreatedAt())
                        .build())
                .toList();
    }

    public List<UserDTO> getAcceptedFriends(String currentUserId) {
        //임의로 넣은 유저 id
        currentUserId = "asdf";
        Integer currentUserSeq = userRepository.findByUserId(currentUserId)
                .orElseThrow().getUserSeq();

        List<FriendEntity> friends = friendRepository
                .findByStatusAndFromUserIdOrStatusAndToUserId("ACCEPTED", currentUserSeq, "ACCEPTED", currentUserSeq);

        return friends.stream()
                .map(f -> {
                    Integer friendSeq = f.getFromUserId().equals(currentUserSeq)
                            ? f.getToUserId() : f.getFromUserId();
                    return userRepository.findById(friendSeq)
                            .map(UserEntity::toDTO).orElse(null);
                })
                .filter(Objects::nonNull)
                .toList();
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
}
