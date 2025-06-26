package com.plit.FO.friend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRepository extends JpaRepository<FriendEntity, Integer> {
    Optional<FriendEntity> findByFriendsNo(Integer friendsNo);
    Optional<FriendEntity> findByStatus(String status);
    Optional<FriendEntity> findByFromUserId(Integer fromUserId);
    Optional<FriendEntity> findByToUserId(Integer toUserId);

    Optional<FriendEntity> findByFromUserIdAndToUserIdAndStatus(Integer fromUserId, Integer toUserId, String status);
    List<FriendEntity> findByToUserIdAndStatus(Integer toUserId, String status);
    List<FriendEntity> findByStatusAndFromUserIdOrStatusAndToUserId(String status1, Integer fromUserId, String status2, Integer toUserId);
}
