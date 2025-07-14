package com.plit.FO.friend.service;

import com.plit.FO.friend.dto.FriendDTO;
import com.plit.FO.friend.entity.FriendEntity;

import java.util.List;

public interface FriendService {
    FriendDTO sendFriendRequest(Integer fromUserId, Integer toUserId, String memo);
    List<FriendDTO> getPendingFriendRequests(Integer currentUserSeq);
    List<FriendDTO> getAcceptedFriends(Integer currentUserSeq);
    void acceptFriendByNo(Integer friendNo, Integer currentUserSeq);
    void declineFriend(Integer friendNo, Integer currentUserSeq);
    void acceptFriend(Integer fromUserId, String currentUserId);
    void updateMemo(Integer friendNo, String memo);
    void blockFriend(Integer friendNo, Integer currentUserSeq);
    void deleteFriend(Integer friendNo, Integer currentUserSeq);
}
