package com.example.proyectoweb.repository;

import com.example.proyectoweb.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByCodigo(String codigo);
    Optional<Usuario> findByDocumento(String documento);
    Optional<Usuario> findByUsername(String username);
    Optional<Usuario> findByEmail(String email);

    @Query("SELECT u FROM Usuario u WHERE LOWER(u.username) = LOWER(:login) OR LOWER(u.codigo) = LOWER(:login) OR LOWER(u.email) = LOWER(:login)")
    Optional<Usuario> findByLoginIdentifier(@Param("login") String login);

    boolean existsByUsername(String username);
    boolean existsByCodigo(String codigo);
}
