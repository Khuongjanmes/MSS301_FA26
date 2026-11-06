package com.mss301.watch.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Chỉ Admin (role = 1) được thêm / sửa / xoá watch và xem Saga.
 * <p>
 * Kiểm tra ở interceptor (chạy TRƯỚC khi Spring đọc + @Valid body) để người không có quyền luôn nhận 403,
 * kể cả khi body sai. Nếu kiểm tra trong thân controller thì @Valid chạy trước -> trả 400 cho người lạ.
 * Header X-User-Role do API Gateway đặt sau khi verify JWT (client tự gửi sẽ bị Gateway xoá).
 */
// [TODO-6.14: chỉ Admin (role=1)]
@Component
public class AdminRoleInterceptor implements HandlerInterceptor {

    public static final String ROLE_HEADER = "X-User-Role";
    private static final String ADMIN_ROLE = "1";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        boolean watchWrite = path.startsWith("/api/watches") && !HttpMethod.GET.matches(request.getMethod());
        boolean sagaRead = path.startsWith("/api/sagas");

        if ((watchWrite || sagaRead) && !ADMIN_ROLE.equals(request.getHeader(ROLE_HEADER))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Only Administrator (role=1) can perform this action");
        }
        return true;
    }
}
