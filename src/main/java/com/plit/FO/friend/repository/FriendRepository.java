package com.plit.FO.friend.repository;

import com.plit.FO.friend.entity.FriendEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRepository extends JpaRepository<FriendEntity, Integer> {

    Optional<FriendEntity> findByFromUserIdAndToUserIdAndStatus(Integer fromUserId, Integer toUserId, String status);
    List<FriendEntity> findByToUserIdAndStatus(Integer toUserId, String status);
    List<FriendEntity> findByStatusAndFromUserIdOrStatusAndToUserId(String status1, Integer fromUserId, String status2, Integer toUserId);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END " +
            "FROM FriendEntity f " +
            "WHERE ((f.fromUserId = :mySeq AND f.toUserId = :targetSeq) " +
            "   OR (f.fromUserId = :targetSeq AND f.toUserId = :mySeq)) " +
            "AND f.status = :status")
    boolean existsByUsersAndStatus(@Param("mySeq") Integer mySeq,
                                   @Param("targetSeq") Integer targetSeq,
                                   @Param("status") String status);
}
