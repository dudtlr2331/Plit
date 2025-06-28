package com.plit.FO.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /// 로그인
    @Transactional(readOnly = true)
    public Optional<UserDTO> loginUser(String userId, String rawPassword) {
        return userRepository.findByUserId(userId)
                .filter(user -> passwordEncoder.matches(rawPassword, user.getUserPwd()))
                .map(this::convertToDto);
    }

    /// 회원가입-아이디 중복확인
    @Transactional(readOnly = true)
    public boolean isUserIdAvailable(String userId) {
        return !userRepository.existsByUserId(userId);
    }

    ///  회원가입-비밀번호 검사
    private boolean isValid(String pw, String email) {
        if (pw.length() < 8) return false;
        int type = 0;
        if (pw.matches(".*[a-zA-Z].*")) type++;
        if (pw.matches(".*\\d.*"))       type++;
        if (pw.matches(".*[^a-zA-Z0-9].*")) type++;
        String idPart = email.split("@")[0];
        return type >= 2 && !pw.contains(idPart);
    }

    /// 랜덤 닉네임 생성 메서드
    private String generateRandomNickname() {
        String[] adjectives = {"용감한", "귀여운", "빛나는", "지혜로운", "날쌘", "우아한", "행복한"};
        String[] nouns = {"토끼", "호랑이", "펭귄", "여우", "늑대", "부엉이", "고양이"};
        Random random = new Random();

        String adj = adjectives[random.nextInt(adjectives.length)];
        String noun = nouns[random.nextInt(nouns.length)];
        int number = 1000 + random.nextInt(9000);

        return adj + noun + number;
    }

    /// 회원가입
    @Transactional
    public UserDTO registerUser(UserDTO userDTO) {
        if (userRepository.existsByUserId(userDTO.getUserId())) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }

        if (!isValid(userDTO.getUserPwd(), userDTO.getUserId())) {
            throw new IllegalArgumentException("비밀번호는 8자 이상, 문자/숫자/기호 중 2개 이상 포함, 이메일 아이디 포함 불가입니다.");
        }

        String encoded = passwordEncoder.encode(userDTO.getUserPwd());

        String nickname;
        do {
            nickname = generateRandomNickname();
        } while (userRepository.existsByUserNickname(nickname));

        UserEntity entity = UserEntity.builder()
                .userId(userDTO.getUserId())
                .userPwd(encoded)
                .userNickname(nickname)
                .useYn("Y")
                .isBanned(false)
                .userAuth("user")
                .userModiId(null)
                .userModiDate(null)
                .userCreateDate(LocalDate.now())
                .build();

        userRepository.save(entity);

        return UserDTO.builder()
                .userId(entity.getUserId())
                .userNickname(entity.getUserNickname())
                .build();
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

        UserEntity existing = userRepository.findById(userSeq)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        if (userDTO.getUserPwd() != null) existing.setUserPwd(passwordEncoder.encode(userDTO.getUserPwd()));
        if (userDTO.getUserNickname() != null) existingUser.setUserNickname(userDTO.getUserNickname());
        if (userDTO.getUseYn() != null) existingUser.setUseYn(userDTO.getUseYn());
        if (userDTO.getIsBanned() != null) existingUser.setIsBanned(userDTO.getIsBanned());
        if (userDTO.getUserAuth() != null) existingUser.setUserAuth(userDTO.getUserAuth());
        if (userDTO.getUserModiId() != null) existingUser.setUserModiId(userDTO.getUserModiId());
        existingUser.setUserModiDate(LocalDate.now());

        existing.setUserModiDate(LocalDate.now());
        return convertToDto(userRepository.save(existing));
    }

    /// 회원탈퇴
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
