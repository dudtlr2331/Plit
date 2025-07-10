package com.plit.FO.config;

import com.plit.FO.user.repository.UserRepository;
import com.plit.FO.user.service.UserServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Map;


@Configuration // 빈 등록
@EnableWebSecurity // Spring Security 설정 활성화
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationEntryPoint customAuthenticationEntryPoint;
    private final UserServiceImpl userServiceImpl;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
//            .csrf(csrf -> csrf.disable()) // 운영 시에는 반드시 CSRF 보호 활성화하기 현재 비성활 중

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/login", "/register", "/signup",
                                "/css/**", "/js/**", "/images/**"
                        ).permitAll()

                        // 로그인한 사용자만 사용 가능하게 설정

                        /* 파티 */
                        .requestMatchers(HttpMethod.POST, "/party/new").authenticated()
                        .requestMatchers(HttpMethod.POST, "/party/edit/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/party/delete/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/parties/*/join").authenticated()

                        /* 클랜 */
                        .requestMatchers(HttpMethod.POST, "/clan/register").authenticated()

                        /* 마이페이지 */
                        .requestMatchers(HttpMethod.POST, "/mypage").authenticated()
                        .requestMatchers(HttpMethod.GET, "/mypage").authenticated()
                        .requestMatchers(HttpMethod.POST, "/mypage/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/friends/**").authenticated()

                        /* 관리자 */
                        .requestMatchers(HttpMethod.PUT, "/api/bo/admin/user/bulk-status").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/bo/admin//report/bulk").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/bo/admin/update/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/bo/admin/delete/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/bo/admin/put/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/bo/admin/report/**").authenticated()

                        .requestMatchers(HttpMethod.POST, "/index").authenticated()
                        .requestMatchers(HttpMethod.POST, "/bo/manage_user").authenticated()
                        .requestMatchers(HttpMethod.POST, "/bo/trol/**").authenticated()

                        .requestMatchers(HttpMethod.GET, "/mypage/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/report").permitAll()

                        // 웹 소켓 사용을 위한 설정
                        .requestMatchers("/ws/**", "/chat/**", "/sub/**", "/pub/**").permitAll()
                        .anyRequest().permitAll() //위에 명시하지 않은 모든 요청은 기본적으로 인증 없이 접근 허용
                )
                // csrf 가 web socket 에 영향을 줄 수 있어 예외 등록
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/ws/**", "/chat/**", "/sub/**", "/pub/**")
                )

                // 비로그인 사용자가 마이페이지 접속시, 로그인 페이지 이동
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.sendRedirect("/login?error=unauthorized");
                        })
                )

                .formLogin(form -> form
                        .loginPage("/login") // 커스텀 로그인 페이지를 /login으로 지정합니다. (GET /login을 처리하는 컨트롤러/HTML 있어야 함)
                        .usernameParameter("userId")   // HTML form의 input name과 맞춰야 함
                        .passwordParameter("userPwd")      // HTML form의 input name과 맞춰야 함
                        .defaultSuccessUrl("/main", true) // 로그인에 성공하는 무조건 이동하는 페이지, true가 있어야 이전 페이지가 무엇이든 관계없이 이 URL로 이동
                        .permitAll() // 로그인 폼과 관련된 URL도 누구나 접근 가능하게 설정
                        .failureHandler((request, response, exception) -> {
                            request.getSession().setAttribute("loginError", "아이디 또는 비밀번호가 잘못되었습니다.");
                            response.sendRedirect("/login");
                        })
                )
                .logout(logout -> logout
                        .logoutUrl("/logout") // 로그아웃 요청 경로
                        .logoutSuccessUrl("/main") // 로그아웃이 완료되면 홈(/)으로 리다이렉트
                        .permitAll()
                )
                
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(userServiceImpl)
                        )
                        .successHandler((request, response, authentication) -> {
                            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

                            Map<String, Object> kakaoAccount = (Map<String, Object>) oAuth2User.getAttributes().get("kakao_account");
                            String email = (String) kakaoAccount.get("email");

                            request.getSession().setAttribute("userEmail", email);
                            response.sendRedirect("/main");
                        })
                );


        return http.build();
    }
}
