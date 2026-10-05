package edu.itm.agrotech.service;

import edu.itm.agrotech.domain.Pedido;
import edu.itm.agrotech.dto.PedidoRequest;

/**
 * Contrato de negocio de Pedido. Los controladores dependen de esta
 * interfaz y no de la implementacion.
 */
public interface PedidoService {

    /**
     * Registra una venta: valida existencias, congela el precio, calcula
     * el total, descuenta el inventario y registra el pago como PENDIENTE.
     *
     * @throws edu.itm.agrotech.exception.ReglaNegocioException si no hay existencias suficientes
     */
    Pedido registrarVenta(PedidoRequest solicitud);

    /** @throws edu.itm.agrotech.exception.RecursoNoEncontradoException si no existe */
    Pedido consultar(Long id);
}
