package edu.itm.agrotech.repository;

import edu.itm.agrotech.entity.Transportador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Repositorio JPA de transportadores. */
public interface TransportadorRepository extends JpaRepository<Transportador, Long> {

    @Query("SELECT t FROM Transportador t JOIN FETCH t.usuario WHERE t.id = :id")
    Optional<Transportador> buscarConUsuario(@Param("id") Long id);

    /** Id del perfil de transportador de un usuario (se usa al iniciar sesion). */
    @Query("SELECT t.id FROM Transportador t WHERE t.usuario.id = :idUsuario")
    Optional<Long> buscarIdPorUsuario(@Param("idUsuario") Long idUsuario);

    /** Todos los transportadores con su usuario ya cargado, por nombre. */
    @Query("SELECT t FROM Transportador t JOIN FETCH t.usuario u ORDER BY u.nombre")
    List<Transportador> listarConUsuario();
}
