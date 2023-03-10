package com.example.lifolio.service;


import com.example.lifolio.base.BaseException;
import com.example.lifolio.base.BaseResponseStatus;
import com.example.lifolio.dto.home.GetGoalRes;
import com.example.lifolio.dto.home.PostGoalReq;
import com.example.lifolio.dto.home.PostGoalRes;
import com.example.lifolio.dto.user.KakaoLoginRes;
import com.example.lifolio.dto.user.*;
import com.example.lifolio.entity.*;
import com.example.lifolio.jwt.JwtFilter;
import com.example.lifolio.jwt.TokenProvider;
import com.example.lifolio.repository.*;
import com.example.lifolio.util.SecurityUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import net.nurigo.java_sdk.api.Message;
import net.nurigo.java_sdk.exceptions.CoolsmsException;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.time.LocalDate;
import java.util.*;

import static com.example.lifolio.base.BaseResponseStatus.NOT_CORRECT_USER;


@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;
    private final GoalOfYearRepository goalOfYearRepository;
    private final CustomLifolioColorRepository customLifolioColorRepository;
    private final CustomLifolioRepository customLifolioRepository;
    private final PasswordEncoder passwordEncoder;
    private final MyFolioRepository myFolioRepository;

    private final TokenProvider tokenProvider;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;

    @Value("${coolsms.key}")
    private String apiKey;
    @Value("${coolsms.secret}")
    private String apiSecret;


    //?????? ????????????(jwt ?????????) ????????? ??????
    public User findNowLoginUser(){
        return SecurityUtil.getCurrentUsername().flatMap(userRepository::findOneWithAuthoritiesByUsername).orElse(null);
    }





    //????????????, ????????? ??????
    public TokenRes login(LoginUserReq loginUserReq) throws BaseException {

        if(!checkUserId(loginUserReq.getUsername())){
            throw new BaseException(BaseResponseStatus.NOT_EXIST_USER);
        }

        User user=userRepository.findByUsername(loginUserReq.getUsername());
        Long userId = user.getId();

        if(!passwordEncoder.matches(loginUserReq.getPassword(),user.getPassword())){
            throw new BaseException(BaseResponseStatus.NOT_CORRECT_PASSWORD);
        }

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginUserReq.getUsername(), loginUserReq.getPassword());

        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);


        String jwt = tokenProvider.createToken(userId); //user???????????? ?????? ??????

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(JwtFilter.AUTHORIZATION_HEADER, "Bearer " + jwt);


        //?????? ??? ????????? ??????
        return new TokenRes(userId,jwt);
    }



    @Transactional
    @SneakyThrows
    public TokenRes signup(SignupUserReq signupUserReq) throws BaseException {

        Authority authority = Authority.builder()
                .authorityName("ROLE_USER")
                .build();

        User user = User.builder()
                .username(signupUserReq.getUsername())
                .password(passwordEncoder.encode(signupUserReq.getPassword()))
                .name(signupUserReq.getName())
                .nickname(signupUserReq.getNickname())
                .phone(signupUserReq.getPhone())
                .authorities(Collections.singleton(authority))
                .activated(true)
                .build();

        Long userId=userRepository.save(user).getId();
        String jwt=tokenProvider.createToken(userId);

        return new TokenRes(userId,jwt);

    }

    @Transactional(readOnly = true)
    public SignupUserReq getUserWithAuthorities(String username) {
        return SignupUserReq.from(userRepository.findOneWithAuthoritiesByUsername(username).orElse(null));
    }

    @Transactional(readOnly = true)
    public SignupUserReq getMyUserWithAuthorities() {
        return SignupUserReq.from(SecurityUtil.getCurrentUsername().flatMap(userRepository::findOneWithAuthoritiesByUsername).orElse(null));
    }


    public boolean checkNickName(String nickName) {
        return userRepository.existsByNickname(nickName);
    }

    public boolean checkUserId(String userId) {
        return userRepository.existsByUsername(userId);
    }




    //???????????? ??????
    @SneakyThrows
    public PasswordRes setNewPassword(PasswordReq passwordReq){ //??? ??????????????? ?????????
        User user = userRepository.findByUsernameEquals(passwordReq.getUsername());
        if(user != null){
            user.setPassword(passwordEncoder.encode(passwordReq.getNewPassword()));
            userRepository.save(user);
            return new PasswordRes(passwordReq.getNewPassword());
        } else {
            return null;
        }

    }




    public int phoneNumberCheck(String to) throws CoolsmsException {

        Message coolsms = new Message(apiKey, apiSecret);

        Random rand  = new Random();
        String numStr = "";
        for(int i=0; i<6; i++) {
            String ran = Integer.toString(rand.nextInt(10));
            numStr+=ran;
        }

        HashMap<String, String> params = new HashMap<String, String>();
        params.put("to", to);    // ??????????????????
        params.put("from", "01049177671");    // ??????????????????. ?????????????????? ??????,?????? ?????? ?????? ????????? ?????? ???
        params.put("type", "sms");
        params.put("text", "[Lifolio] ??????????????? [" + numStr + "] ?????????.");


        try {
            JSONObject obj = (JSONObject) coolsms.send(params);
            System.out.println(obj.toString());
        } catch (CoolsmsException e) {
            System.out.println(e.getMessage());
            System.out.println(e.getCode());
        }
        return Integer.parseInt(numStr);
    }

    public String findUserId(FindUserIdReq findUserIdReq) throws BaseException {
        User user =userRepository.findByNameAndPhone(findUserIdReq.getName(),findUserIdReq.getPhone());
        if(user==null){
            throw new BaseException(NOT_CORRECT_USER);
        }
        return user.getUsername();
    }


    public KakaoLoginRes createKakaoUser(String token) throws BaseException {

        String reqURL = "https://kapi.kakao.com/v2/user/me";
        String profileImgUrl="";
        String nickname="";
        String email = "";

        //access_token??? ???????????? ????????? ?????? ??????
        try {
            URL url = new URL(reqURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Authorization", "Bearer " + token); //????????? header ??????, access_token??????


            int responseCode = conn.getResponseCode();

            if (responseCode==401){
                throw new BaseException(BaseResponseStatus.INVALID_ACCESS_TOKEN);
            }

            //????????? ?????? ?????? JSON????????? Response ????????? ????????????
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = "";
            String result = "";

            while ((line = br.readLine()) != null) {
                result += line;
            }
            System.out.println("response body : " + result);

            //Gson ?????????????????? JSON ??????
            JsonParser parser = new JsonParser();
            JsonElement element = parser.parse(result);


            //kakao????????? ?????? ?????????
            profileImgUrl = element.getAsJsonObject().get("kakao_account").getAsJsonObject().get("profile").getAsString();
            nickname = element.getAsJsonObject().get("kakao_account").getAsJsonObject().get("name").getAsString();
            email = element.getAsJsonObject().get("kakao_account").getAsJsonObject().get("email").getAsString();

            br.close();

//            //?????? ????????? & ?????????????????? ?????? ID??? ????????????
//            if (!checkNickName(nickname)){
//
//            }
//
//            else {
//
//            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        //?????? ????????? ??????
        return new KakaoLoginRes(100, nickname, profileImgUrl, "kakao");


    }

}