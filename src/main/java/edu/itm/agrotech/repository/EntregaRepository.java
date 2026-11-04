package edu.itm.agrotech.repository;

import edu.itm.agrotech.domain.EstadoEntrega;
import edu.itm.agrotech.entity.Entrega;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Repositorio JPA de entregas. */
public interface EntregaRepository extends JpaRepository<Entrega, Long> {

    boolean existsByIdPedido(Long idPedido);

    Optional<Entrega> findByIdPedido(Long idPedido);

    /** Entrega del pedido con su transportador y el usuario de este ya cargados. */
    @Query("""
            SELECT e FROM Entrega e
            LEFT JOIN FETCH e.transportador t
            LEFT JOIN FETCH t.usuario
            WHERE e.idPedido = :idPedido
            """)
    Optional<Entrega> buscarPorPedido(@Param("idPedido") Long idPedido);

    /** Entregas de un transportador por fecha programada; el estado es opcional. */
    @Query("""
            SELECT e FROM Entrega e
            JOIN FETCH e.transportador t
            JOIN FETCH t.usuario
            WHERE t.id = :idTransportador
              AND (:estado IS NULL OR e.estado = :estado)
            ORDER BY e.fechaProgramada, e.id
            """)
    List<Entrega> listarPorTransportador(@Param("idTransportador") Long idTransportador,
                                         @Param("estado") EstadoEntrega estado);
}
