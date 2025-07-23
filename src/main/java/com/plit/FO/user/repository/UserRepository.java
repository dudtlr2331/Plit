package com.plit.FO.user.repository;

import com.plit.FO.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Integer> {
    Optional<UserEntity> findByUserId(String userId);
    Optional<UserEntity> findByUserSeq(Integer userSeq);
    Optional<UserEntity> findByUserNickname(String userNickname);

    @Query("SELECT u FROM UserEntity u WHERE REPLACE(u.userNickname, ' ', '') = :normalizedNickname")
    Optional<UserEntity> findByNormalizedNickname(@Param("normalizedNickname") String normalizedNickname);

    boolean existsByUserId(String userId);
    boolean existsByUserNickname(String userNickname);
    List<UserEntity> findByUserNicknameContaining(String keyword);
    void deleteById(Integer userSeq);
    UserEntity save(UserEntity user);

}
