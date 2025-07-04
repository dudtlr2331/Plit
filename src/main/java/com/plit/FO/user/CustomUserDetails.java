package com.plit.FO.user;

import com.plit.FO.user.UserDTO;
import org.springframework.security.core.userdetails.User;
import java.util.List;

public class CustomUserDetails extends User {

    private final UserDTO userDTO;

    public CustomUserDetails(UserDTO userDTO) {
        super(userDTO.getUserId(), userDTO.getUserPwd(), List.of());
        this.userDTO = userDTO;
    }

    public UserDTO getUserDTO() {
        return userDTO;
    }

    public String getUserNickname() {
        return userDTO.getUserNickname();
    }
}
