package com.mss301.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Xác thực tập trung ở Gateway: verify chữ ký + hạn JWT rồi gắn X-User-Id / X-User-Role cho service phía sau.
 * Service phía sau TIN header này, nên Gateway phải luôn xoá bản do client tự gửi.
 */
// [TODO-7.3, TODO-7.4, TODO-7.5: verify JWT, chống giả mạo header, public chỉ GET /api/watches]
@Component
@Slf4j
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";
    private static final String BEARER = "Bearer ";

    /**
     * Các đường dẫn KHÔNG cần JWT (mọi method), so khớp CHÍNH XÁC.
     * logout là Public theo đặc tả API (Readme 3.2); MSAccount không lưu phiên nên logout không cần token.
     */
    private static final Set<String> PUBLIC_PATHS = Set.of(
        "/api/auth/login",
        "/api/auth/refresh",
        "/api/auth/logout"
    );

    private final JwtParser jwtParser;

    public JwtAuthFilter(@Value("${jwt.secret}") String jwtSecret) {
        // Tạo parser 1 lần. Keys.hmacShaKeyFor ném lỗi nếu secret < 32 byte -> phát hiện cấu hình sai ngay lúc khởi động.
        this.jwtParser = Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
            .build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // (1) LUÔN xoá X-User-* do client tự gửi lên -> chống giả mạo "X-User-Role: 1".
        //     Chỉ Gateway mới được phép đặt các header này sau khi verify JWT.
        ServerHttpRequest sanitized = request.mutate()
            .headers(h -> {
                h.remove(USER_ID_HEADER);
                h.remove(USER_ROLE_HEADER);
            })
            .build();

        // (2) Public: login/refresh/logout, fallback và CHỈ "GET /api/watches" (đề bài: list không cần quyền).
        //     KHÔNG dùng startsWith("/api/watches"): như vậy POST/PUT/DELETE cũng thành public.
        if (isPublic(request.getMethod(), path)) {
            return chain.filter(exchange.mutate().request(sanitized).build());
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER)) {
            return unauthorized(exchange, "Missing Bearer token");
        }

        Claims claims;
        try {
            claims = jwtParser.parseSignedClaims(authHeader.substring(BEARER.length())).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            // chữ ký sai, hết hạn, token hỏng...
            return unauthorized(exchange, "Invalid or expired token");
        }

        if (!"access".equals(claims.get("type", String.class))) {     // refresh token không được dùng để gọi API
            return unauthorized(exchange, "Token is not an access token");
        }
        Object role = claims.get("role");
        if (claims.getSubject() == null || role == null) {
            return unauthorized(exchange, "Token is missing user information");
        }

        // (3) Gắn thông tin user cho downstream service
        ServerHttpRequest authenticated = sanitized.mutate()
            .header(USER_ID_HEADER, claims.getSubject())
            .header(USER_ROLE_HEADER, String.valueOf(role))
            .build();
        return chain.filter(exchange.mutate().request(authenticated).build());
    }

    private boolean isPublic(HttpMethod method, String path) {
        return PUBLIC_PATHS.contains(path)
            || path.startsWith("/fallback/")
            || (HttpMethod.GET.equals(method) && path.equals("/api/watches"));
    }

    /** 401 với body JSON cùng định dạng ApiResponse của các service. */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = ("{\"success\":false,\"message\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1; // chạy trước các filter khác
    }
}
