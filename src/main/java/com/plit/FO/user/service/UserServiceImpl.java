package com.plit.FO.user.service;

import com.plit.FO.user.dto.UserDTO;
import com.plit.FO.user.entity.UserEntity;
import com.plit.FO.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends DefaultOAuth2UserService implements UserService {

    private final JavaMailSender   mailSender;
    private final UserRepository   userRepository;
    private final PasswordEncoder  passwordEncoder;
    private final Map<String, Long>   emailSendTimeMap = new ConcurrentHashMap<>();
    private final Map<String, String> emailCodeMap     = new ConcurrentHashMap<>();

    /* ---------- 인증 ---------- */

    @Override
    public String sendEmailCode(String email) {
        long now      = System.currentTimeMillis();
        long lastTime = emailSendTimeMap.getOrDefault(email, 0L);

        if (now - lastTime < 3 * 60 * 1000) {
            throw new IllegalStateException("인증번호는 3분 후에 다시 요청할 수 있습니다.");
        }

        String code = generateCode();
        emailCodeMap.put(email, code);
        emailSendTimeMap.put(email, now);

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(email);
        msg.setSubject("회원가입 인증번호");
        msg.setText("인증번호: " + code + "\n\n(유효시간 3분)");
        msg.setFrom("noreply@fo.com");

        mailSender.send(msg);
        return code;
    }

    @Override
    public boolean verifyEmailCode(String email, String inputCode) {
        String saved  = emailCodeMap.get(email);
        Long   sentAt = emailSendTimeMap.get(email);
        if (saved == null || sentAt == null) return false;
        if (System.currentTimeMillis() - sentAt > 3 * 60 * 1000) return false;
        return saved.equals(inputCode);
    }

    /* ---------- 로그인 / 가입 ---------- */

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDTO> loginUser(String userId, String rawPassword) {
        return userRepository.findByUserId(userId)
                .filter(u -> passwordEncoder.matches(rawPassword, u.getUserPwd()))
                .map(this::toDTO);
    }

    @Override
    @Transactional
    public UserDTO registerUser(UserDTO dto) {
        if (userRepository.existsByUserId(dto.getUserId()))
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");

        if (!isValid(dto.getUserPwd(), dto.getUserId()))
            throw new IllegalArgumentException("비밀번호 규칙 오류");

        String nickname;
        do { nickname = generateRandomNickname(); }
        while (userRepository.existsByUserNickname(nickname));

        UserEntity saved = userRepository.save(UserEntity.builder()
                .userId(dto.getUserId())
                .userPwd(passwordEncoder.encode(dto.getUserPwd()))
                .userNickname(nickname)
                .useYn("Y")
                .isBanned(false)
                .userAuth("user")
                .userCreateDate(LocalDate.now())
                .build());

        return UserDTO.builder()
                .userId(saved.getUserId())
                .userNickname(saved.getUserNickname())
                .build();
    }

    @Override
    public boolean changePassword(String userId, String currentPwd, String newPwd) {
        return userRepository.findByUserId(userId)
                .filter(u -> passwordEncoder.matches(currentPwd, u.getUserPwd()))
                .map(u -> {
                    u.setUserPwd(passwordEncoder.encode(newPwd));
                    return userRepository.save(u);
                }).isPresent();
    }

    /* ---------- 조회 ---------- */

    @Override
    @Transactional(readOnly = true)
    public boolean isUserIdAvailable(String userId) {
        return !userRepository.existsByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDTO> getUserByUserId(String userId) {
        return userRepository.findByUserId(userId).map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDTO> getUserBySeq(Integer userSeq) {
        return userRepository.findById(userSeq).map(this::toDTO);
    }

    @Override
    public UserDTO findByUserId(String userId) {
        return userRepository.findByUserId(userId)
                .map(UserEntity::toDTO)
                .orElse(null);
    }

    /* ---------- 수정 / 삭제 ---------- */

    @Override
    @Transactional
    public UserDTO updateUser(Integer userSeq, UserDTO dto) {
        UserEntity user = userRepository.findById(userSeq)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        if (dto.getUserPwd() != null)
            user.setUserPwd(passwordEncoder.encode(dto.getUserPwd()));

        if (dto.getUserNickname() != null && !dto.getUserNickname().equals(user.getUserNickname())) {
            if (userRepository.existsByUserNickname(dto.getUserNickname()))
                throw new IllegalArgumentException("이미 존재하는 닉네임입니다.");
            user.setUserNickname(dto.getUserNickname());
        }

        if (dto.getUseYn()     != null) user.setUseYn(dto.getUseYn());
        if (dto.getIsBanned()  != null) user.setIsBanned(dto.getIsBanned());
        if (dto.getUserAuth()  != null) user.setUserAuth(dto.getUserAuth());
        if (dto.getUserModiId()!= null) user.setUserModiId(dto.getUserModiId());

        user.setUserModiDate(LocalDate.now());
        return toDTO(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteUser(Integer userSeq) {
        if (!userRepository.existsById(userSeq))
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userSeq);
        userRepository.deleteById(userSeq);
    }

    @Override
    @Transactional
    public void updateNickname(String userId, String newNickname) {
        UserEntity user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
        user.setUserNickname(newNickname);
        user.setUserModiDate(LocalDate.now());
        userRepository.save(user);
    }

    @Override
    public void updateUserStatus(Integer userSeq, String action) {
        UserEntity user = userRepository.findById(userSeq)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        switch (action) {
            case "BAN"     -> user.setIsBanned(true);
            case "UNBAN"   -> user.setIsBanned(false);
            case "BLOCK"   -> user.setUseYn("N");
            case "UNBLOCK" -> user.setUseYn("Y");
            case "DELETE"  -> { userRepository.deleteById(userSeq); return; }
            default        -> throw new IllegalArgumentException("유효하지 않은 액션입니다.");
        }
        userRepository.save(user);
    }

    @Override
    public void updateUserInfo(Integer userSeq, String nickname, String auth) {
        UserEntity user = userRepository.findById(userSeq)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setUserNickname(nickname);
        user.setUserAuth(auth);
        userRepository.save(user);
    }

    /* ---------- 검색 ---------- */

    @Override
    public List<UserDTO> searchByNickname(String keyword) {
        return userRepository.findByUserNicknameContaining(keyword)
                .stream()
                .map(UserEntity::toDTO)
                .collect(Collectors.toList());
    }

    /* ---------- 내부 유틸 ---------- */

    /** 비밀번호 규칙 검사 */
    private boolean isValid(String pw, String email) {
        if (pw.length() < 8) return false;
        int score = (pw.matches(".*[a-zA-Z].*") ? 1 : 0)
                + (pw.matches(".*\\d.*")      ? 1 : 0)
                + (pw.matches(".*[^a-zA-Z0-9].*") ? 1 : 0);
        String idPart = email.split("@")[0];
        return score >= 2 && !pw.contains(idPart);
    }

    /** 닉네임 자동 생성 */
    private String generateRandomNickname() {
        String[] adj  = {"암흑의","불꽃의","서리 내린","고요한","광기의","신속한","잊혀진","밤하늘의","차원의","황혼의"};
        String[] noun = {"탈리아","야스오","제드","아리","이렐리아","카타리나","아칼리","모르가나","카직스","에코","브랜드","릴리아","세라핀","샤코","피들스틱"};
        ThreadLocalRandom r = ThreadLocalRandom.current();
        return adj[r.nextInt(adj.length)] + noun[r.nextInt(noun.length)] + r.nextInt(1000, 10000);
    }

    /** 인증번호 6자리 생성 */
    private String generateCode() {
        return "%06d".formatted(ThreadLocalRandom.current().nextInt(1_000_000));
    }

    /** Entity → DTO */
    private UserDTO toDTO(UserEntity e) {
        return UserDTO.builder()
                .userSeq(e.getUserSeq())
                .userId(e.getUserId())
                .userPwd(e.getUserPwd())
                .userNickname(e.getUserNickname())
                .useYn(e.getUseYn())
                .isBanned(e.getIsBanned())
                .userModiId(e.getUserModiId())
                .userModiDate(e.getUserModiDate())
                .userCreateDate(e.getUserCreateDate())
                .userAuth(e.getUserAuth())
                .build();
    }

    /// 카카오 로그인
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest)
            throws OAuth2AuthenticationException {

        // 1. 카카오 API 호출 → 사용자 정보 가져오기
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String provider = userRequest.getClientRegistration().getRegistrationId(); // "kakao"
        Map<String, Object> attributes = oAuth2User.getAttributes();

        /* 2. 카카오 계정 정보 파싱 */
        if ("kakao".equals(provider)) {
            Map<String, Object> kakaoAccount =
                    (Map<String, Object>) attributes.get("kakao_account");
            Map<String, Object> profile =
                    (Map<String, Object>) kakaoAccount.get("profile");

            String email    = (String) kakaoAccount.get("email");      // ← userId 로 사용
            String nickname = (String) profile.get("nickname");

            /* 3. DB 에 사용자 저장(없으면) */
            processOAuthPostLogin(email, nickname);
        }

        /* 4. Spring Security 인증 객체 반환 */
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "id"   // Kakao user-id attribute
        );
    }

    /** OAuth2 로그인 후 신규 사용자 처리 */
    private void processOAuthPostLogin(String email, String nickname) {
        userRepository.findByUserId(email)
                .orElseGet(() -> userRepository.save(
                        UserEntity.builder()
                                .userId(email)
                                .userPwd(passwordEncoder.encode(UUID.randomUUID().toString()))
                                .userNickname(
                                        nickname != null ? nickname : generateRandomNickname()
                                )
                                .useYn("Y")
                                .isBanned(false)
                                .userAuth("user")
                                .userCreateDate(LocalDate.now())
                                .build()
                ));
    }
}
