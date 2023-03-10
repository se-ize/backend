package com.example.lifolio.dto.user;

import lombok.*;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginUserReq {

    private String username;

    private String password;
}
