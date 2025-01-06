//package com.example.chatverse.infrastructure.configuration;
//
//import com.example.chatverse.domain.service.CustomUserDetailsService;
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.RequiredArgsConstructor;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
//import org.springframework.stereotype.Component;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import java.io.IOException;
//
//@Component
//@RequiredArgsConstructor
//public class JwtAuthenticationFilter extends OncePerRequestFilter {
//
//    private final JwtService jwtService; // Класс для работы с JWT токенами
//    private final CustomUserDetailsService customUserDetailsService; // Загрузка данных о пользователе
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest request,
//                                    HttpServletResponse response,
//                                    FilterChain filterChain)
//            throws ServletException, IOException {
//        final String authHeader = request.getHeader("Authorization");
//        final String jwt;
//        final String username;
//
//        // Проверяем наличие токена в заголовке Authorization
//        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
//            filterChain.doFilter(request, response);
//            return;
//        }
//
//        jwt = authHeader.substring(7); // Убираем "Bearer " из заголовка
//        username = jwtService.extractUsername(jwt); // Извлекаем имя пользователя из токена
//
//        // Проверяем, если пользователь уже аутентифицирован
//        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
//            var userDetails = customUserDetailsService.loadUserByUsername(username);
//
//            // Проверяем валидность токена
//            if (jwtService.isTokenValid(jwt, userDetails)) {
//                var authToken = new UsernamePasswordAuthenticationToken(
//                        userDetails,
//                        null,
//                        userDetails.getAuthorities()
//                );
//
//                // Устанавливаем дополнительные детали (например, IP-адрес)
//                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//
//                // Устанавливаем аутентификацию в SecurityContext
//                SecurityContextHolder.getContext().setAuthentication(authToken);
//            }
//        }
//
//        // Передаём запрос дальше в цепочке фильтров
//        filterChain.doFilter(request, response);
//    }
//}
