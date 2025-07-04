package com.plit.FO.user;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.stream.Collectors;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class UserService {

    private final JavaMailSender mailSender;
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
        String[] adjectives = {"암흑의", "불꽃의", "서리 내린", "고요한", "광기의", "신속한", "잊혀진", "밤하늘의", "차원의", "황혼의"};
        String[] nouns = {"탈리아", "야스오", "제드", "아리", "이렐리아", "카타리나", "아칼리", "모르가나", "카직스", "에코", "브랜드", "릴리아", "세라핀", "샤코", "피들스틱"};
        Random random = new Random();

        String adj = adjectives[random.nextInt(adjectives.length)];
        String noun = nouns[random.nextInt(nouns.length)];
        int number = 1000 + random.nextInt(9000);

        return adj + noun + number;
    }

    /// 회원가입 - 인증번호 메일 발송
    public String sendEmailCode(String toEmail) {
        String code = String.format("%06d", new Random().nextInt(1_000_000));
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(toEmail);
        msg.setSubject("회원가입 인증번호");
        msg.setText("인증번호: " + code + " (유효시간 3분)");
        msg.setFrom("doormouse149@gmail.com");
        mailSender.send(msg);
        return code;          // 컨트롤러에서 세션에 저장
    }


    /// 관리자 페이지 - 유저 조회
    public UserDTO findByUserId(String userId) {
        return userRepository.findByUserId(userId)
                .map(UserEntity::toDTO)
                .orElse(null);
    }


    /// 회원가입 - 인증번호 검증
    public boolean verifyEmailCode(String inputCode, String savedCode) {
        return savedCode != null && savedCode.equals(inputCode);
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

    /// 비밀번호 변경
    public boolean changePassword(String userId, String currentPwd, String newPwd) {
        Optional<UserEntity> userOpt = userRepository.findByUserId(userId);
        if (userOpt.isPresent() && passwordEncoder.matches(currentPwd, userOpt.get().getUserPwd())) {
            UserEntity user = userOpt.get();
            user.setUserPwd(passwordEncoder.encode(newPwd));
            userRepository.save(user);
            return true;
        }
        return false;
    }


    /// 닉네임 변경
    @Transactional
    public void updateNickname(String userId, String newNickname) {
        UserEntity user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        user.setUserNickname(newNickname);
        user.setUserModiDate(LocalDate.now());
        userRepository.save(user);
    }


    // 관리자 창에서 유저의 상태 환경을 변경할 때 사용
    public void updateUserStatus(Integer userSeq, String action) {
        UserEntity user = userRepository.findById(userSeq)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        switch (action) {
            case "BAN" -> user.setIsBanned(true);
            case "UNBAN" -> user.setIsBanned(false);
            case "BLOCK" -> user.setUseYn("N");
            case "UNBLOCK" -> user.setUseYn("Y");
            case "DELETE" -> {
                userRepository.deleteById(userSeq);
                return; // 삭제 후 save 필요 없음
            }
            default -> throw new IllegalArgumentException("유효하지 않은 액션입니다.");
        }
        userRepository.save(user);
    }

    // 유저관리에서 닉네임 검색용으로 사용
    public List<UserDTO> searchByNickname(String keyword) {
        return userRepository.findByUserNicknameContaining(keyword)
                .stream()
                .map(UserEntity::toDTO) // 또는 직접 DTO로 매핑
                .collect(Collectors.toList());
    }

    //  관리자 페이지에서 유저 정보 수정 - 닉네임, 권한
    public void updateUserInfo(Integer userSeq, String nickname, String auth) {
        UserEntity user = userRepository.findById(userSeq)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setUserNickname(nickname);
        user.setUserAuth(auth);
        userRepository.save(user);
    }
}
