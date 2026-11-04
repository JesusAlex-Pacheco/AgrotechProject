package edu.itm.agrotech.repository;

import edu.itm.agrotech.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio JPA de usuarios. */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** Consulta derivada del nombre del metodo: Spring Data arma el JPQL. */
    boolean existsByCorreoIgnoreCase(String correo);
}
