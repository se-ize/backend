package com.example.lifolio.dto.user;

import com.example.lifolio.entity.User;
import lombok.*;

import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SignupUserReq {

    private String username;

    //@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private String nickname;

    private String name;

    private String phone;


    //이건 requestBody 입력할 때, 입력할 필요 없음
    private Set<AuthorityRes> authorityResSet;

    public static SignupUserReq from(User user) {
        if(user == null) return null;

        return SignupUserReq.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .nickname(user.getNickname())
                .name(user.getName())
                .name(user.getPhone())
                .authorityResSet(user.getAuthorities().stream()
                        .map(authority -> AuthorityRes.builder().authorityName(authority.getAuthorityName()).build())
                        .collect(Collectors.toSet()))
                .build();
    }

}