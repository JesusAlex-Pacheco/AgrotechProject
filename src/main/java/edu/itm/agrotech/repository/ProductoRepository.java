package edu.itm.agrotech.repository;

import edu.itm.agrotech.domain.Producto;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia de Producto. La capa de negocio depende de esta
 * interfaz y no de la implementacion, de modo que un cambio de motor de base
 * de datos no afecte la logica del sistema.
 */
public interface ProductoRepository {

    Producto guardar(Producto producto);

    Optional<Producto> buscarPorId(Long id);

    List<Producto> listar(Long idCategoria, String nombre);

    boolean actualizar(Producto producto);

    boolean desactivar(Long id);
}
