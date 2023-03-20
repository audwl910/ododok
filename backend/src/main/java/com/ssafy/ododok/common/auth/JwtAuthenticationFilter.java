package com.ssafy.ododok.common.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.ododok.api.dto.LoginRequestDto;
import com.ssafy.ododok.api.dto.RefreshTokenDto;
import com.ssafy.ododok.db.model.RefreshToken;
import com.ssafy.ododok.db.model.User;
import com.ssafy.ododok.db.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter{

    private final AuthenticationManager authenticationManager;
    private final RefreshTokenRepository refreshTokenRepository;


    // Authentication 객체 만들어서 리턴 => 의존 : AuthenticationManager
    // 인증 요청시에 실행되는 함수 => /login
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {

        System.out.println("JwtAuthenticationFilter : 진입");

        // request에 있는 username과 password를 파싱해서 자바 Object로 받기
        ObjectMapper om = new ObjectMapper();
        LoginRequestDto loginRequestDto = null;
        try {
            loginRequestDto = om.readValue(request.getInputStream(), LoginRequestDto.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("JwtAuthenticationFilter : "+loginRequestDto);

        // 유저네임패스워드 토큰 생성
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(
                        loginRequestDto.getUserEmail(),
                        loginRequestDto.getPassword());

        System.out.println("JwtAuthenticationFilter : 토큰생성완료");

        // authenticate() 함수가 호출 되면 인증 프로바이더가 유저 디테일 서비스의
        // loadUserByUsername(토큰의 첫번째 파라메터) 를 호출하고
        // UserDetails를 리턴받아서 토큰의 두번째 파라메터(credential)과
        // UserDetails(DB값)의 getPassword()함수로 비교해서 동일하면
        // Authentication 객체를 만들어서 필터체인으로 리턴해준다.

        // Tip: 인증 프로바이더의 디폴트 서비스는 UserDetailsService 타입
        // Tip: 인증 프로바이더의 디폴트 암호화 방식은 BCryptPasswordEncoder
        // 결론은 인증 프로바이더에게 알려줄 필요가 없음.
        Authentication authentication =
                authenticationManager.authenticate(authenticationToken);

        PrincipalDetails principalDetailis = (PrincipalDetails) authentication.getPrincipal();
        System.out.println("Authentication : "+principalDetailis.getUser().getUserEmail());
        return authentication;
    }

    // JWT Token 생성해서 response에 담아주기
    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
                                            Authentication authResult) throws IOException, ServletException {

        PrincipalDetails principalDetailis = (PrincipalDetails) authResult.getPrincipal();

        String jwtToken = JWT.create()
                .withSubject(principalDetailis.getUsername())
                .withExpiresAt(new Date(System.currentTimeMillis()+JwtProperties.EXPIRATION_TIME))
                .withClaim("email", principalDetailis.getUser().getUserEmail())
                .withClaim("nickname", principalDetailis.getUser().getUserNickname())
                .sign(Algorithm.HMAC512(JwtProperties.SECRET));

        response.setHeader(JwtProperties.ACCESS_HEADER_STRING, JwtProperties.TOKEN_PREFIX+jwtToken);

        String email = principalDetailis.getUser().getUserEmail();

        String refreshToken = JWT.create()
                .withSubject(principalDetailis.getUsername())
                .withExpiresAt(new Date(System.currentTimeMillis()+JwtProperties.REFRESH_TIME))
                .sign(Algorithm.HMAC512(JwtProperties.SECRET));

        RefreshToken refreshToken1 = new RefreshToken();
        refreshToken1.setEmail(email);
        refreshToken1.setRefreshToken(refreshToken);
        refreshTokenRepository.save(refreshToken1);

        response.setHeader(JwtProperties.REFRESH_HEADER_STRING, JwtProperties.TOKEN_PREFIX+refreshToken);

        Map<String, String> responseMap = new HashMap<>();
        responseMap.put(JwtProperties.ACCESS_HEADER_STRING, jwtToken);
        responseMap.put(JwtProperties.REFRESH_HEADER_STRING, refreshToken);
        new ObjectMapper().writeValue(response.getWriter(), responseMap);

    }

//    @Override
//    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
//
//        setFailReason(response, failed.getMessage());
//    }
//
//    private void setFailReason(HttpServletResponse response, String message) throws IOException {
//        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
//        response.setContentType("application/json;charset=UTF-8");
//
//        JSONObject jsonObject = new JSONObject();
//        jsonObject.put("success", false);
//        jsonObject.put("code", 400);
//        jsonObject.put("message", message);
//
//        response.getWriter().print(jsonObject);
//    }

}
