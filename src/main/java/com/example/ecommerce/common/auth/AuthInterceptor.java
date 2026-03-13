package com.example.ecommerce.common.auth;

import com.example.ecommerce.common.error.ErrorCode;
import com.example.ecommerce.common.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    public AuthInterceptor(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!requiresLogin(handler)) {
            return true;
        }

        String authHeader = request.getHeader(AUTH_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "未登录或 token 缺失");
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "token 不能为空");
        }

        try {
            UserPrincipal principal = jwtTokenProvider.parseToken(token);
            AuthContext.set(principal);
            return true;
        } catch (Exception ex) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "token 无效或已过期");
        }
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        AuthContext.clear();
    }

    private boolean requiresLogin(Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return false;
        }
        return handlerMethod.hasMethodAnnotation(LoginRequired.class)
                || handlerMethod.getBeanType().isAnnotationPresent(LoginRequired.class);
    }
}
