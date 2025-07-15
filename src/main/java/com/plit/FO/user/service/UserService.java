package com.plit.FO.user.service;

import com.plit.FO.user.dto.UserDTO;
import com.plit.FO.user.entity.UserEntity;

import java.util.List;
import java.util.Optional;

public interface UserService {

    /* ---------- 인증 ---------- */
    String sendEmailCode(String email);
    boolean verifyEmailCode(String email, String inputCode);

    /* ---------- 로그인 / 가입 ---------- */
    Optional<UserDTO> loginUser(String userId, String rawPassword);
    UserDTO registerUser(UserDTO userDTO);
    boolean changePassword(String userId, String currentPwd, String newPwd);

    /* ---------- 조회 ---------- */
    boolean isUserIdAvailable(String userId);
    List<UserDTO> getAllUsers();
    Optional<UserDTO> getUserByUserId(String userId);
    Optional<UserDTO> getUserBySeq(Integer userSeq);
    UserDTO findByUserId(String userId);
    Optional<UserEntity> findByUserNickname(String nickname);

    /* ---------- 수정 / 삭제 ---------- */
    UserDTO updateUser(Integer userSeq, UserDTO userDTO);
    void    deleteUser(Integer userSeq);
    void    updateNickname(String userId, String newNickname);
    void    updateUserStatus(Integer userSeq, String action);
    void    updateUserInfo(Integer userSeq, String nickname, String auth);
    boolean isValidPassword(String password, String userId);
    boolean checkPassword(String userId, String rawPassword);
    void resetPassword(String email, String newPwd);


    /* ---------- 검색 ---------- */
    List<UserDTO> searchByNickname(String keyword);


}
