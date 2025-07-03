package com.plit.FO.config;

import com.plit.FO.user.CustomUserDetailsService;
import com.plit.FO.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration // 빈 등록
@EnableWebSecurity // Spring Security 설정 활성화
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
//            .csrf(csrf -> csrf.disable()) // 운영 시에는 반드시 CSRF 보호 활성화하기 현재 비성활 중

            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(
                            "/login", "/register",
                            "/css/**", "/js/**", "/images/**"  // 이 경로는 로그인하지 않아도 접근 허용
                    ).permitAll()

                    .requestMatchers(HttpMethod.POST, "/party/new").authenticated()
                    .requestMatchers(HttpMethod.POST, "/party/edit/**").authenticated()
                    .requestMatchers(HttpMethod.POST, "/party/delete/**").authenticated() // 로그인한 사용자만 사용 가능하게 설정
                    .requestMatchers(HttpMethod.POST, "/clan/register").authenticated()

                    .anyRequest().permitAll() //위에 명시하지 않은 모든 요청은 기본적으로 인증 없이 접근 허용
            )
            .formLogin(form -> form
                    .loginPage("/login") // 커스텀 로그인 페이지를 /login으로 지정합니다. (GET /login을 처리하는 컨트롤러/HTML 있어야 함)
                    .usernameParameter("userId")   // HTML form의 input name과 맞춰야 함
                    .passwordParameter("userPwd")      // HTML form의 input name과 맞춰야 함
                    .defaultSuccessUrl("/main", true) // 로그인에 성공하는 무조건 이동하는 페이지, true가 있어야 이전 페이지가 무엇이든 관계없이 이 URL로 이동
                    .permitAll() // 로그인 폼과 관련된 URL도 누구나 접근 가능하게 설정
            )
            .logout(logout -> logout
                    .logoutUrl("/logout") // 로그아웃 요청 경로
                    .logoutSuccessUrl("/") // 로그아웃이 완료되면 홈(/)으로 리다이렉트
                    .permitAll()
            );

        return http.build();
    }
}
