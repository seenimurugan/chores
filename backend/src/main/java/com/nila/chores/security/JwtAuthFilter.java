package com.nila.chores.security;

import com.nila.chores.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtService jwt;

    public JwtAuthFilter(JwtService jwt) { this.jwt = jwt; }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims c = jwt.parse(token);
                Long uid = Long.valueOf(c.getSubject());
                String username = c.get("username", String.class);
                String name = c.get("name", String.class);
                User.Role role = User.Role.valueOf(c.get("role", String.class));
                AuthUser principal = new AuthUser(uid, username, name, role);
                var auth = new UsernamePasswordAuthenticationToken(
                        principal, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (ExpiredJwtException e) {
                String jti = safeJti(e.getClaims());
                log.warn("event=jwt.invalid reason=expired token={} path={}", jti, req.getRequestURI());
                SecurityContextHolder.clearContext();
            } catch (SignatureException e) {
                log.warn("event=jwt.invalid reason=signature path={}", req.getRequestURI());
                SecurityContextHolder.clearContext();
            } catch (MalformedJwtException e) {
                log.warn("event=jwt.invalid reason=malformed path={}", req.getRequestURI());
                SecurityContextHolder.clearContext();
            } catch (JwtException | IllegalArgumentException e) {
                log.warn("event=jwt.invalid reason=parse-error detail={} path={}", e.getMessage(), req.getRequestURI());
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(req, res);
    }

    private String safeJti(Claims claims) {
        if (claims == null) return "unknown";
        String jti = claims.getId();
        return jti != null ? jti : "no-jti";
    }
}
