package edu.itm.agrotech.service;

import edu.itm.agrotech.domain.Producto;
import edu.itm.agrotech.dto.ProductoRequest;

import java.util.List;

/**
 * Contrato de negocio de Producto. Los controladores dependen de esta
 * interfaz y no de la implementacion, de modo que las reglas puedan
 * cambiar o probarse por separado sin tocar la capa de presentacion.
 */
public interface ProductoService {

    /**
     * Crea un producto. Nace disponible solo si tiene existencias.
     *
     * @throws edu.itm.agrotech.exception.RecursoNoEncontradoException si el agricultor o la categoria no existen
     */
    Producto crear(ProductoRequest solicitud);

    /** @throws edu.itm.agrotech.exception.RecursoNoEncontradoException si no existe */
    Producto consultar(Long id);

    /** Lista el catalogo con filtros opcionales por categoria y por nombre. */
    List<Producto> listar(Long idCategoria, String nombre);

    /**
     * Actualiza los datos editables del producto.
     *
     * @throws edu.itm.agrotech.exception.ReglaNegocioException si se intenta cambiar el agricultor
     * @throws edu.itm.agrotech.exception.RecursoNoEncontradoException si el producto o la categoria no existen
     */
    Producto actualizar(Long id, ProductoRequest solicitud);

    /**
     * Baja logica: el producto deja de mostrarse en el catalogo pero
     * se conserva porque puede estar referenciado en pedidos historicos.
     */
    void desactivar(Long id);
}
