package com.plit.FO.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Integer> {
    Optional<UserEntity> findByUserId(String userId);
    Optional<UserEntity> findByUserNickname(String userNickname);
    boolean existsByUserId(String userId);
    boolean existsByUserNickname(String userNickname);
    List<UserEntity> findByUserNicknameContaining(String keyword);
    void deleteById(Integer userSeq);
    UserEntity save(UserEntity user);
}
