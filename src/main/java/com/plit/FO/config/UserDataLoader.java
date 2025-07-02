package com.plit.FO.config;

import com.plit.FO.user.UserEntity;
import com.plit.FO.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Random;

@Configuration
public class UserDataLoader {

    @Bean
    CommandLineRunner loadDataUser(UserRepository repository, PasswordEncoder passwordEncoder) {

        return args -> {
            if (repository.count() > 0) return;

            String[] adj  = {"용감한","귀여운","빛나는","지혜로운","날쌘","우아한","행복한","엉뚱한"};
            String[] noun = {"토끼","호랑이","펭귄","여우","늑대","부엉이","고양이","판다","도치","수달"};
            Random rnd = new Random();

            for (int i = 1; i <= 20; i++) {
                String email = "test" + i + "@example.com";
                String pwd = "password" + i;
                String hashPw = passwordEncoder.encode(pwd);

                String nickname = adj[rnd.nextInt(adj.length)]
                        + noun[rnd.nextInt(noun.length)]
                        + (1000 + rnd.nextInt(9000));

                repository.save(UserEntity.builder()
                        .userId(email)
                        .userPwd(hashPw)
                        .userNickname(nickname)
                        .useYn("Y")
                        .isBanned(false)
                        .userAuth("USER") // 대문자로 들어가야함
                        .userModiId(null)
                        .userModiDate(null)
                        .userCreateDate(LocalDate.now())
                        .build());
            }
        };
    }

}
