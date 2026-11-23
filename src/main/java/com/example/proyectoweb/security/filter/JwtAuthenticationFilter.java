package com.example.proyectoweb.security.filter;

import com.example.proyectoweb.model.Usuario;
import com.example.proyectoweb.repository.UsuarioRepository;
import com.example.proyectoweb.security.TokenJwtConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.*;

import static com.example.proyectoweb.security.TokenJwtConfig.*;

public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthenticationFilter(AuthenticationManager authenticationManager, UsuarioRepository usuarioRepository) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        setFilterProcessesUrl("/login");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {

        String username = null;
        String password = null;

        try {
            Map<?, ?> credentials = new ObjectMapper().readValue(request.getInputStream(), Map.class);
            if (credentials != null) {
                Object u = credentials.get("username");
                Object p = credentials.get("password");
                if (u != null) username = u.toString().trim();
                if (p != null) password = p.toString();
            }
        } catch (Exception ignored) {
            // Si no viene como JSON en el body, intentar leer parámetros estándar del request
            username = request.getParameter("username");
            password = request.getParameter("password");
        }

        if (username == null) username = "";
        if (password == null) password = "";

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(username, password);

        return authenticationManager.authenticate(authenticationToken);
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain chain, Authentication authResult) throws IOException, ServletException {

        org.springframework.security.core.userdetails.User springUser =
                (org.springframework.security.core.userdetails.User) authResult.getPrincipal();

        String username = springUser.getUsername();
        Collection<? extends GrantedAuthority> authorities = authResult.getAuthorities();

        List<String> roles = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        // Obtener datos complementarios del usuario para el frontend
        Optional<Usuario> usuarioOpt = usuarioRepository.findByLoginIdentifier(username);

        Long idUsuario = usuarioOpt.map(Usuario::getIdUsuario).orElse(null);
        String nombre = usuarioOpt.map(Usuario::getNombre).orElse(username);
        String rolPrincipal = roles.isEmpty() ? "ROLE_USER" : roles.get(0);
        String documento = usuarioOpt.map(Usuario::getDocumento).orElse("");
        String email = usuarioOpt.map(Usuario::getEmail).orElse("");

        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + TokenJwtConfig.EXPIRATION_TIME);

        // Claims mínimos dentro del JWT: únicamente identidad y autoridades indispensables
        Claims claims = Jwts.claims()
                .subject(username)
                .add("authorities", roles)
                .add("username", username)
                .add("rol", rolPrincipal)
                .build();

        String token = Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(SECRET_KEY)
                .compact();

        // Cabecera Authorization para compatibilidad con Angular AuthService y el interceptor
        response.addHeader(HEADER_AUTHORIZATION, PREFIX_TOKEN + token);

        // Cuerpo JSON completo para facilitar la integración directa con Angular
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("token", token);
        body.put("type", "Bearer");
        body.put("username", username);
        body.put("idUsuario", idUsuario);
        body.put("nombre", nombre);
        body.put("rol", rolPrincipal);
        body.put("authorities", roles);
        body.put("roles", roles);
        body.put("documento", documento);
        body.put("email", email);
        body.put("expiresIn", TokenJwtConfig.EXPIRATION_TIME);
        body.put("message", String.format("¡Bienvenido, %s! Has iniciado sesión exitosamente.", nombre));

        response.getWriter().write(new ObjectMapper().writeValueAsString(body));
        response.setContentType(CONTENT_TYPE);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                              AuthenticationException failed) throws IOException, ServletException {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("error", "Error en la autenticación");
        body.put("message", "Nombre de usuario o contraseña incorrectos.");

        response.getWriter().write(new ObjectMapper().writeValueAsString(body));
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(CONTENT_TYPE);
    }
}
