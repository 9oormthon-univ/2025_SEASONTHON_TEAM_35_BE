package com.growplan.global.jwt.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.growplan.global.error.ApiResponse;
import com.growplan.global.error.code.BaseErrorCode;
import com.growplan.global.error.code.ErrorReasonDTO;
import com.growplan.global.error.code.status.ErrorStatus;
import com.growplan.global.error.exception.GeneralException;
import com.growplan.global.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            // 1. HttpServletRequest에 있는 header에서 Authorization header를 가져와 토큰을 가져온다.
            String accessToken = jwtUtil.getAccessTokenFromHeader(request.getHeader("Authorization"));

            if (accessToken == null || accessToken.isEmpty()) {
                filterChain.doFilter(request, response);
                return;
            }

            // 2. jwtUtil을 이용하여 토큰에서 memberId를 가져온다.
            Long memberId = jwtUtil.getIdFromToken(accessToken);

            // 3. UserDetailService를 이용하여 UserDetail 객체를 가져온다.
            UserDetails details = userDetailsService.loadUserByUsername(memberId.toString());
            if (details == null) {
                throw new GeneralException(ErrorStatus.MEMBER_NOT_FOUND);
            }

            // 4. 해당 객체를 SecurityContextHolder에 넣어준다
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(details,
                    null, // 비밀번호는 필요 없으므로 null 처리
                    details.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);
        } catch (GeneralException e) {
            BaseErrorCode code = e.getCode();
            ErrorReasonDTO reason = e.getErrorReasonHttpStatus();
            response.setStatus(reason.getHttpStatus().value());
            response.setContentType("application/json; charset=UTF-8");

            ApiResponse<Object> customResponse = ApiResponse.onFailure(code.getReason().getCode(),
                    code.getReason().getMessage(), null);

            ObjectMapper om = new ObjectMapper();
            om.writeValue(response.getOutputStream(), customResponse);
        }
    }
}
