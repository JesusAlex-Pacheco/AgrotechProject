package edu.itm.agrotech.repository;

import edu.itm.agrotech.domain.Categoria;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia de Categoria. La capa de negocio depende de esta
 * interfaz y no de la implementacion JDBC.
 */
public interface CategoriaRepository {

    Categoria guardar(Categoria categoria);

    Optional<Categoria> buscarPorId(Long id);

    Optional<Categoria> buscarPorNombre(String nombre);

    List<Categoria> listar();

    boolean actualizar(Categoria categoria);

    boolean eliminar(Long id);

    /** Indica si algun producto (disponible o no) pertenece a la categoria. */
    boolean tieneProductos(Long id);
}
