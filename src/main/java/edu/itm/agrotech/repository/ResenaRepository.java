package edu.itm.agrotech.repository;

import edu.itm.agrotech.entity.Resena;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Repositorio JPA de resenas. */
public interface ResenaRepository extends JpaRepository<Resena, Long> {

    boolean existsByClienteIdAndIdProducto(Long idCliente, Long idProducto);

    /** Resenas de un producto, de la mas reciente a la mas antigua, con el nombre de quien la escribio. */
    @Query("""
            SELECT r FROM Resena r
            JOIN FETCH r.cliente c
            JOIN FETCH c.usuario
            WHERE r.idProducto = :idProducto
            ORDER BY r.fecha DESC, r.id DESC
            """)
    List<Resena> listarPorProducto(@Param("idProducto") Long idProducto);
}
