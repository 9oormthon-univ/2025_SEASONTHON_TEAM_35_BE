package com.growplan.global.config;

import com.growplan.global.jwt.filter.JwtFilter;
import com.growplan.global.jwt.handler.JwtAccessDeniedHandler;
import com.growplan.global.jwt.handler.JwtAuthenticationEntryPoint;
import com.growplan.global.oauth.handler.OAuthLoginFailureHandler;
import com.growplan.global.oauth.handler.OAuthLoginSuccessHandler;
import com.growplan.global.oauth.service.PrincipalOauth2UserService;
import com.growplan.global.util.JwtUtil;
import jakarta.servlet.Filter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HttpBasicConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // OAuth2 로그인 성공 시 handler
    private final OAuthLoginSuccessHandler oAuthLoginSuccessHandler;
    // OAuth2 로그인 실패 시 handler
    private final OAuthLoginFailureHandler oAuthLoginFailureHandler;
    // OAuth2 유저 처리 handler
    private final PrincipalOauth2UserService principalOauth2UserService;
    // 인가에 실패한 경우 실행할 예외 처리 handler
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    // 인증에 실패한 경우 실행할 예외 처리 Handler
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtUtil jwtProvider;
    private final UserDetailsService userDetailsService;

    // 허용할 URL을 배열의 형태로 관리
    public static final String[] allowUrl = {
            "/",
            "/swagger-ui/**",
            "/swagger-resources/**",
            "/v3/api-docs/**",
            "/api/v0/auth/kakao",
            "/oauth2/**",
            "/api/v0/member/success",
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, CorsConfig corsConfig) throws Exception {
        http
                // 허용할 URL, 역할별로 나눌 URL, 인증을 요구하는 URL 설정
                .authorizeHttpRequests(request -> request
                        // allowUrl을 모두 허용
                        .requestMatchers(allowUrl).permitAll()
                        // 이외의 요청에 대해서는 인증이 필요하도록 설정
                        .anyRequest().authenticated())
                // cors 필터 추가
                .addFilter(corsConfig.corsFilter())
                // jwtFilter를 UsernamePasswordAuthenticationFilter 앞에 오도록 설정
                .addFilterBefore(jwtFilter(), UsernamePasswordAuthenticationFilter.class)
                // formLogin 비활성화
                .formLogin(AbstractHttpConfigurer::disable)
                // httpBasic 비활성화
                .httpBasic(HttpBasicConfigurer::disable)
                // csrf 비활성화
                .csrf(AbstractHttpConfigurer::disable)
                // 인증 인가에 대한 예외처리
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        // 인가에 대해 예외처리할 Handler 추가
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                        // 인증에 대해 예외처리할 Handler 추가
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint))
                // OAuth2에 대한 처리
                .oauth2Login(oauth ->
                        oauth
                                .successHandler(oAuthLoginSuccessHandler) // 로그인 성공 시 핸들러
                                .failureHandler(oAuthLoginFailureHandler) // 로그인 실패 시 핸들러
                                .userInfoEndpoint(userInfo ->
                                        userInfo.userService(principalOauth2UserService) // OAuth2 사용자 서비스 설정
                                )
                );


        return http.build();
    }

    @Bean
    public Filter jwtFilter() {
        return new JwtFilter(jwtProvider, userDetailsService);
    }
}
