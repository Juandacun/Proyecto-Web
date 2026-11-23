package com.example.proyectoweb.security.filter;

import com.example.proyectoweb.model.Usuario;
import com.example.proyectoweb.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import java.io.IOException;
import java.util.*;

import static com.example.proyectoweb.security.TokenJwtConfig.*;

public class JwtValidationFilter extends BasicAuthenticationFilter {

    private final UsuarioRepository usuarioRepository;

    public JwtValidationFilter(AuthenticationManager authenticationManager) {
        this(authenticationManager, null);
    }

    public JwtValidationFilter(AuthenticationManager authenticationManager, UsuarioRepository usuarioRepository) {
        super(authenticationManager);
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws IOException, ServletException {

        String header = request.getHeader(HEADER_AUTHORIZATION);

        // Si no hay cabecera de autenticación o no es Bearer, delegar al resto de la cadena
        if (header == null || !header.startsWith(PREFIX_TOKEN)) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.replace(PREFIX_TOKEN, "").trim();

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(SECRET_KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String username = claims.getSubject();

            Collection<GrantedAuthority> authorities = new ArrayList<>();

            // Si el repositorio está disponible, consultar el rol en vivo de la base de datos
            // para evitar que un cambio de rol persista indebidamente durante la vigencia del token
            if (usuarioRepository != null) {
                Optional<Usuario> usuarioOpt = usuarioRepository.findByLoginIdentifier(username);
                if (usuarioOpt.isEmpty()) {
                    throw new JwtException("El usuario asociado al token no existe o ha sido deshabilitado.");
                }
                Usuario usuario = usuarioOpt.get();
                String rolActual = usuario.getRol();
                if (rolActual == null || rolActual.isBlank()) {
                    rolActual = "ROLE_USER";
                } else if (!rolActual.startsWith("ROLE_")) {
                    rolActual = "ROLE_" + rolActual;
                }
                authorities.add(new SimpleGrantedAuthority(rolActual));
            } else {
                Object authoritiesRaw = claims.get("authorities");
                if (authoritiesRaw == null) {
                    authoritiesRaw = claims.get("roles");
                }

                if (authoritiesRaw instanceof List<?> list) {
                    for (Object item : list) {
                        if (item != null) {
                            String authStr = item.toString();
                            if (!authStr.startsWith("ROLE_")) {
                                authStr = "ROLE_" + authStr;
                            }
                            authorities.add(new SimpleGrantedAuthority(authStr));
                        }
                    }
                } else if (authoritiesRaw != null) {
                    String authStr = authoritiesRaw.toString();
                    if (!authStr.startsWith("ROLE_")) {
                        authStr = "ROLE_" + authStr;
                    }
                    authorities.add(new SimpleGrantedAuthority(authStr));
                }
            }

            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            chain.doFilter(request, response);

        } catch (JwtException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
            body.put("error", "Token JWT inválido o expirado");
            body.put("message", e.getMessage());

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(CONTENT_TYPE);
            response.getWriter().write(new ObjectMapper().writeValueAsString(body));
        }
    }
}
