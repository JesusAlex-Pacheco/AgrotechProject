package edu.itm.agrotech.repository;

import edu.itm.agrotech.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/** Repositorio JPA de clientes. */
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    @Query("SELECT c FROM Cliente c JOIN FETCH c.usuario WHERE c.id = :id")
    Optional<Cliente> buscarConUsuario(@Param("id") Long id);

    /** Cliente con su usuario y sus resenas (relacion @OneToMany) ya cargados. */
    @Query("SELECT c FROM Cliente c JOIN FETCH c.usuario LEFT JOIN FETCH c.resenas WHERE c.id = :id")
    Optional<Cliente> buscarConResenas(@Param("id") Long id);
}
