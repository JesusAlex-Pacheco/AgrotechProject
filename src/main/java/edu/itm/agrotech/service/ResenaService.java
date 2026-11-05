package edu.itm.agrotech.service;

import edu.itm.agrotech.dto.ResenaRequest;
import edu.itm.agrotech.entity.Resena;

import java.util.List;

/** Contrato de negocio de las resenas de productos. */
public interface ResenaService {

    /**
     * Registra la calificacion de un cliente a un producto.
     *
     * @throws edu.itm.agrotech.exception.RecursoNoEncontradoException si el producto o el cliente no existen
     * @throws edu.itm.agrotech.exception.ReglaNegocioException si el cliente no ha recibido el producto
     *         o ya lo habia resenado
     */
    Resena registrar(Long idProducto, ResenaRequest solicitud);

    /**
     * Resenas de un producto, de la mas reciente a la mas antigua.
     *
     * @throws edu.itm.agrotech.exception.RecursoNoEncontradoException si el producto no existe
     */
    List<Resena> listarPorProducto(Long idProducto);

    /**
     * Resenas escritas por un cliente, de la mas reciente a la mas antigua.
     *
     * @throws edu.itm.agrotech.exception.RecursoNoEncontradoException si el cliente no existe
     */
    List<Resena> listarPorCliente(Long idCliente);
}
