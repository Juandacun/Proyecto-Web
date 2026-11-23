package com.example.proyectoweb.security;

import com.example.proyectoweb.model.Usuario;
import com.example.proyectoweb.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class JpaUserDetailsService implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByLoginIdentifier(login)
                .orElseThrow(() -> new UsernameNotFoundException(
                        String.format("Usuario '%s' no encontrado en el sistema", login)));

        String rol = usuario.getRol();
        if (rol == null || rol.isBlank()) {
            rol = "ROLE_USER";
        }
        if (!rol.startsWith("ROLE_")) {
            rol = "ROLE_" + rol.toUpperCase();
        }

        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(rol));

        return new User(
                usuario.getUsername(),
                usuario.getPassword() != null ? usuario.getPassword() : "",
                true,
                true,
                true,
                true,
                authorities
        );
    }
}
