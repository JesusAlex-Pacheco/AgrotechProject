package edu.itm.agrotech.repository;

import edu.itm.agrotech.entity.Agricultor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Repositorio JPA de agricultores. Spring Data genera la implementacion:
 * save, findById, existsById... y las consultas declaradas aqui.
 */
public interface AgricultorRepository extends JpaRepository<Agricultor, Long> {

    /** Trae el agricultor con su usuario en una sola consulta (JOIN FETCH). */
    @Query("SELECT a FROM Agricultor a JOIN FETCH a.usuario WHERE a.id = :id")
    Optional<Agricultor> buscarConUsuario(@Param("id") Long id);
}
