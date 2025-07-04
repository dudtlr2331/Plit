package com.plit.FO.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        // 1. userId로 UserEntity 찾기
        UserEntity entity = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자 없음: " + userId));

        // 2. UserEntity → UserDTO
        UserDTO userDTO = entity.toDTO();

        // 3. CustomUserDetails로 래핑해서 반환
        return new CustomUserDetails(userDTO);
    }
}
