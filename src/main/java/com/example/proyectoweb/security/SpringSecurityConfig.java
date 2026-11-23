package com.example.proyectoweb.security;

import com.example.proyectoweb.repository.UsuarioRepository;
import com.example.proyectoweb.security.filter.JwtAuthenticationFilter;
import com.example.proyectoweb.security.filter.JwtValidationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SpringSecurityConfig {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CustomAuthenticationEntryPoint authenticationEntryPoint;

    @Autowired
    private CustomAccessDeniedHandler accessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {
        JwtAuthenticationFilter authFilter = new JwtAuthenticationFilter(authenticationManager, usuarioRepository);
        authFilter.setFilterProcessesUrl("/login");

        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        // 1. Recursos estáticos, documentación OpenAPI y vistas públicas
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // 2. Autenticación pública
                        .requestMatchers(HttpMethod.POST, "/login", "/api/auth/**").permitAll()

                        // 3. Catálogo público / consulta inicial de recursos, categorías y ubicaciones
                        .requestMatchers(HttpMethod.GET, "/catalogo", "/recursos/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/recursos/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categorias/**", "/categorias/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/ubicaciones/**", "/ubicaciones/**").permitAll()

                        // 4. Perfil del usuario autenticado (cualquier rol autenticado)
                        .requestMatchers(HttpMethod.GET, "/api/usuarios/me", "/usuarios/me").authenticated()

                        // 5. Gestión administrativa (exclusiva ROLE_ADMIN)
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/usuarios/**", "/usuarios/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/categorias/**", "/categorias/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/categorias/**", "/categorias/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/categorias/**", "/categorias/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/ubicaciones/**", "/ubicaciones/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/ubicaciones/**", "/ubicaciones/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/ubicaciones/**", "/ubicaciones/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/recursos/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/recursos/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/recursos/**").hasRole("ADMIN")

                        // 5. Gestión operativa y recursos (ROLE_GESTOR y ROLE_ADMIN)
                        .requestMatchers(HttpMethod.PATCH, "/api/recursos/{id}/estado").hasAnyRole("ADMIN", "GESTOR")
                        .requestMatchers("/api/mantenimientos/**", "/mantenimientos/**").hasAnyRole("ADMIN", "GESTOR")
                        .requestMatchers("/tablero/**", "/api/indicadores/**").hasAnyRole("ADMIN", "GESTOR")
                        .requestMatchers(HttpMethod.POST, "/api/prestamos/**", "/prestamos/**").hasAnyRole("ADMIN", "GESTOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/prestamos/**", "/prestamos/**").hasAnyRole("ADMIN", "GESTOR")
                        .requestMatchers(HttpMethod.PUT, "/api/prestamos/**", "/prestamos/**").hasAnyRole("ADMIN", "GESTOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/prestamos/**", "/prestamos/**").hasAnyRole("ADMIN", "GESTOR")

                        // 6. Consultas de préstamos (autenticado, con verificación de propietario para solicitantes)
                        .requestMatchers(HttpMethod.GET, "/api/prestamos/**", "/prestamos/**").authenticated()

                        // 7. Gestión de reservas (autenticado, con reglas de propietario para ROLE_USER)
                        .requestMatchers("/api/reservas/**", "/reservas/**").authenticated()

                        // 8. Reporte y consulta de incidentes (autenticado)
                        .requestMatchers("/api/incidentes/**", "/incidentes/**").authenticated()

                        // 9. Cualquier otra petición debe estar autenticada
                        .anyRequest().authenticated()
                )
                .addFilter(authFilter)
                .addFilter(new JwtValidationFilter(authenticationManager, usuarioRepository))
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(Arrays.asList("*"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "X-Requested-With", "Origin"));
        config.setExposedHeaders(List.of("Authorization", "Content-Type", "Access-Control-Allow-Origin", "Access-Control-Allow-Credentials"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistrationBean() {
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(corsConfigurationSource()));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}
