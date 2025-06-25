package com.plit.FO.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // --- User Registration/Login related ---
    @Transactional
    public UserDTO registerUser(UserDTO userDTO) {
        if (userRepository.existsByUserId(userDTO.getUserId())) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }
        if (userRepository.existsByUserNickname(userDTO.getUserNickname())) {
            throw new IllegalArgumentException("이미 존재하는 닉네임입니다.");
        }

        // In a real application, userDTO.getUserPwd() should be hashed here
        UserEntity userEntity = UserEntity.builder()
                .userId(userDTO.getUserId())
                .userPwd(userDTO.getUserPwd()) // TODO: Implement password hashing (e.g., BCryptPasswordEncoder)
                .userNickname(userDTO.getUserNickname())
                .useYn("Y")
                .isBanned(false)
                .userModiId(userDTO.getUserId()) // For initial creation, modifier is creator
                .userModiDate(LocalDate.now())
                .userCreateDate(LocalDate.now())
                .userAuth("user")
                .build();

        UserEntity savedUser = userRepository.save(userEntity);
        return convertToDto(savedUser);
    }

    @Transactional(readOnly = true)
    public Optional<UserDTO> loginUser(String userId, String rawPassword) {
        // In a real application, compare hashed passwords
        return userRepository.findByUserId(userId)
                .filter(user -> user.getUserPwd().equals(rawPassword)) // TODO: Compare hashed password here
                .map(this::convertToDto);
    }

    // --- General User Management (can be used by BO/FO) ---
    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<UserDTO> getUserByUserId(String userId) {
        return userRepository.findByUserId(userId).map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public Optional<UserDTO> getUserBySeq(Integer userSeq) {
        return userRepository.findById(userSeq).map(this::convertToDto);
    }

    @Transactional
    public UserDTO updateUser(Integer userSeq, UserDTO userDTO) {
        UserEntity existingUser = userRepository.findById(userSeq)
                .orElseThrow(() -> new IllegalArgumentException("해당하는 사용자를 찾을 수 없습니다: " + userSeq));

        if (userDTO.getUserNickname() != null &&
                !userDTO.getUserNickname().equals(existingUser.getUserNickname()) &&
                userRepository.existsByUserNickname(userDTO.getUserNickname())) {
            throw new IllegalArgumentException("이미 존재하는 닉네임입니다.");
        }

        if (userDTO.getUserPwd() != null) existingUser.setUserPwd(userDTO.getUserPwd()); // TODO: Hash new password
        if (userDTO.getUserNickname() != null) existingUser.setUserNickname(userDTO.getUserNickname());
        if (userDTO.getUseYn() != null) existingUser.setUseYn(userDTO.getUseYn());
        if (userDTO.getIsBanned() != null) existingUser.setIsBanned(userDTO.getIsBanned());
        if (userDTO.getUserAuth() != null) existingUser.setUserAuth(userDTO.getUserAuth());
        if (userDTO.getUserModiId() != null) existingUser.setUserModiId(userDTO.getUserModiId());
        existingUser.setUserModiDate(LocalDate.now());

        UserEntity updatedUser = userRepository.save(existingUser);
        return convertToDto(updatedUser);
    }

    @Transactional
    public void deleteUser(Integer userSeq) {
        if (!userRepository.existsById(userSeq)) {
            throw new IllegalArgumentException("해당하는 사용자를 찾을 수 없습니다: " + userSeq);
        }
        userRepository.deleteById(userSeq);
    }

    private UserDTO convertToDto(UserEntity userEntity) {
        return UserDTO.builder()
                .userSeq(userEntity.getUserSeq())
                .userId(userEntity.getUserId())
                .userPwd(userEntity.getUserPwd())
                .userNickname(userEntity.getUserNickname())
                .useYn(userEntity.getUseYn())
                .isBanned(userEntity.getIsBanned())
                .userModiId(userEntity.getUserModiId())
                .userModiDate(userEntity.getUserModiDate())
                .userCreateDate(userEntity.getUserCreateDate())
                .userAuth(userEntity.getUserAuth())
                .build();
    }
}
