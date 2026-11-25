package edu.itm.agrotech.repository;

import edu.itm.agrotech.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Repositorio JPA de usuarios. */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** Consulta derivada del nombre del metodo: Spring Data arma el JPQL. */
    boolean existsByCorreoIgnoreCase(String correo);

    /** Usuario por su correo, sin importar mayusculas (se usa al iniciar sesion). */
    Optional<Usuario> findByCorreoIgnoreCase(String correo);
}
