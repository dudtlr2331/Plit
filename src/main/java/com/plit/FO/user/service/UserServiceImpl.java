package com.plit.FO.user.service;

import com.plit.FO.matchHistory.dto.riot.RiotAccountResponse;
import com.plit.FO.user.dto.UserDTO;
import com.plit.FO.user.entity.UserEntity;
import com.plit.FO.user.enums.EmailVerificationPurpose;
import com.plit.FO.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends DefaultOAuth2UserService implements UserService {

    @Value("${riot.api.key}")
    private String riotApiKey;

    private final RestTemplate restTemplate;
    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Map<String, String> emailCodeMap = new ConcurrentHashMap<>();
    private final Map<String, Long> emailSendTimeMap = new ConcurrentHashMap<>();
    private final Set<String> verifiedSignupEmails = ConcurrentHashMap.newKeySet();

    /* ---------- 인증 ---------- */

    private String getKey(String email, EmailVerificationPurpose purpose) {
        return email + ":" + purpose.name();
    }

    @Override
    public String sendEmailCode(String email, EmailVerificationPurpose purpose) {
        String key = getKey(email, purpose);
        long now = System.currentTimeMillis();
        long lastTime = emailSendTimeMap.getOrDefault(key, 0L);

        if (now - lastTime < 3 * 60 * 1000) {
            throw new IllegalStateException("인증번호는 3분 후에 다시 요청할 수 있습니다.");
        }

        String code = generateCode();
        emailCodeMap.put(key, code);
        emailSendTimeMap.put(key, now);

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(email);
        msg.setSubject("[" + purpose.name() + "] 인증번호");
        msg.setText("인증번호: " + code + "\n\n(유효시간 3분)");
        msg.setFrom("noreply@fo.com");

        mailSender.send(msg);
        return code;
    }

    @Override
    public boolean verifyEmailCode(String email, String inputCode, EmailVerificationPurpose purpose) {
        String key = getKey(email, purpose);
        String saved = emailCodeMap.get(key);
        Long sentAt = emailSendTimeMap.get(key);

        if (saved == null || sentAt == null) return false;
        if (System.currentTimeMillis() - sentAt > 3 * 60 * 1000) return false;

        boolean matched = saved.equals(inputCode);
        if (matched && purpose == EmailVerificationPurpose.SIGNUP) {
            verifiedSignupEmails.add(email);
        }

        return matched;
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
        if (!verifiedSignupEmails.contains(dto.getUserId())) {
            throw new IllegalArgumentException("회원가입을 위해 이메일 인증을 먼저 완료해주세요.");
        }

        if (userRepository.existsByUserId(dto.getUserId()))
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");

        if (!isValid(dto.getUserPwd(), dto.getUserId()))
            throw new IllegalArgumentException("비밀번호 규칙 오류");

        String nickname;
        do { nickname = generateRandomNickname(); }
        while (userRepository.existsByUserNickname(nickname));

        verifiedSignupEmails.remove(dto.getUserId());

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

    public UserDTO findByUserSeq(Integer userSeq) {
        return userRepository.findByUserSeq(userSeq)
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
    public Optional<UserEntity> findByUserNickname(String nickname) {
        return userRepository.findByUserNickname(nickname);
    }

    @Override
    @Transactional
    public void updateNickname(String userId, String newNickname) {
        if (userRepository.existsByUserNickname(newNickname)) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

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

    @Override
    public boolean isValidPassword(String password, String userId) {
        if (password == null || password.length() < 8) return false;

        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasNumber = password.matches(".*\\d.*");
        boolean hasSpecial = password.matches(".*[^a-zA-Z0-9].*");
        int typeCount = (hasLetter ? 1 : 0) + (hasNumber ? 1 : 0) + (hasSpecial ? 1 : 0);

        String emailPrefix = userId.contains("@") ? userId.split("@")[0] : userId;
        boolean includesEmail = password.contains(emailPrefix);

        return typeCount >= 2 && !includesEmail;
    }

    @Override
    public boolean checkPassword(String userId, String rawPassword) {
        UserEntity user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
        return passwordEncoder.matches(rawPassword, user.getUserPwd());
    }

    @Override
    public void resetPassword(String email, String newPwd) {
        Optional<UserEntity> userOpt = userRepository.findByUserId(email);

        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("해당 이메일의 사용자를 찾을 수 없습니다.");
        }

        UserEntity user = userOpt.get();
        String encoded = passwordEncoder.encode(newPwd);
        user.setUserPwd(encoded);
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

    private boolean isValid(String pw, String email) {
        if (pw.length() < 8) return false;
        int score = (pw.matches(".*[a-zA-Z].*") ? 1 : 0)
                + (pw.matches(".*\\d.*")      ? 1 : 0)
                + (pw.matches(".*[^a-zA-Z0-9].*") ? 1 : 0);
        String idPart = email.split("@")[0];
        return score >= 2 && !pw.contains(idPart);
    }

    private String generateRandomNickname() {
        String[] adj  = {"암흑의","불꽃의","서리 내린","고요한","광기의","신속한","잊혀진","밤하늘의","차원의","황혼의"};
        String[] noun = {"탈리아","야스오","제드","아리","이렐리아","카타리나","아칼리","모르가나","카직스","에코","브랜드","릴리아","세라핀","샤코","피들스틱"};
        ThreadLocalRandom r = ThreadLocalRandom.current();
        return adj[r.nextInt(adj.length)] + noun[r.nextInt(noun.length)] + r.nextInt(1000, 10000);
    }

    private String generateCode() {
        return "%06d".formatted(ThreadLocalRandom.current().nextInt(1_000_000));
    }

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

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String provider = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        if ("kakao".equals(provider)) {
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

            String email = (String) kakaoAccount.get("email");
            String nickname = (String) profile.get("nickname");

            if (email == null) {
                throw new OAuth2AuthenticationException("카카오 계정에 이메일이 존재하지 않습니다.");
            }

            processOAuthPostLogin(email, nickname);

            Map<String, Object> enrichedAttributes = new HashMap<>(attributes);
            enrichedAttributes.put("email", email);

            return new DefaultOAuth2User(
                    Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                    enrichedAttributes,
                    "email"
            );
        }

        throw new OAuth2AuthenticationException("지원하지 않는 OAuth2 공급자입니다: " + provider);
    }

    private void processOAuthPostLogin(String email, String unusedNicknameFromKakao) {
        userRepository.findByUserId(email)
                .orElseGet(() -> {
                    String nickname;
                    do {
                        nickname = generateRandomNickname();
                    } while (userRepository.existsByUserNickname(nickname));

                    return userRepository.save(
                            UserEntity.builder()
                                    .userId(email)
                                    .userPwd(passwordEncoder.encode(UUID.randomUUID().toString()))
                                    .userNickname(nickname)
                                    .useYn("Y")
                                    .isBanned(false)
                                    .userAuth("user")
                                    .userCreateDate(LocalDate.now())
                                    .build()
                    );
                });
    }

    @Override
    @Transactional
    public boolean verifyAndSaveSummoner(String userId, String summonerName, String tagLine) {
        RiotAccountResponse response = getAccountByRiotId(summonerName, tagLine);
        if (response == null) return false;

        UserEntity user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        user.setRiotGameName(response.getGameName());
        user.setRiotTagLine(response.getTagLine());
        user.setPuuid(response.getPuuid());
        user.setUserModiDate(LocalDate.now());

        userRepository.save(user);
        return true;
    }

    @Override
    public RiotAccountResponse getAccountByRiotId(String gameName, String tagLine) {
        String encodedGameName = UriUtils.encodePathSegment(gameName, StandardCharsets.UTF_8);
        String encodedTagLine = UriUtils.encodePathSegment(tagLine, StandardCharsets.UTF_8);

        String url = "https://asia.api.riotgames.com/riot/account/v1/accounts/by-riot-id/" + encodedGameName + "/" + encodedTagLine;

        URI uri = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("api_key", riotApiKey)
                .build()
                .toUri();

        try {
            return restTemplate.getForObject(uri, RiotAccountResponse.class);
        } catch (Exception e) {
            return null;
        }
    }


    @Override
    @Transactional
    public void deleteSummonerInfo(String userId) {
        UserEntity user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        user.setRiotGameName(null);
        user.setRiotTagLine(null);
        user.setPuuid(null);
        user.setUserModiDate(LocalDate.now());
        userRepository.save(user);
    }
}
