package edu.itm.agrotech.service;

import edu.itm.agrotech.domain.Categoria;
import edu.itm.agrotech.dto.CategoriaRequest;

import java.util.List;

/**
 * Contrato de negocio de Categoria. Los controladores dependen de esta
 * interfaz y no de la implementacion.
 */
public interface CategoriaService {

    /** @throws edu.itm.agrotech.exception.ReglaNegocioException si el nombre ya existe */
    Categoria crear(CategoriaRequest solicitud);

    /** @throws edu.itm.agrotech.exception.RecursoNoEncontradoException si no existe */
    Categoria consultar(Long id);

    List<Categoria> listar();

    /** @throws edu.itm.agrotech.exception.ReglaNegocioException si el nombre choca con otra categoria */
    Categoria actualizar(Long id, CategoriaRequest solicitud);

    /**
     * Borrado fisico: la tabla categoria no tiene columna de estado.
     * Regla: no se elimina una categoria que tenga productos, porque
     * esos productos quedarian sin clasificar.
     */
    void eliminar(Long id);
}
