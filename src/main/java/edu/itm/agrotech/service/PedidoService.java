package edu.itm.agrotech.service;

import edu.itm.agrotech.domain.EstadoPedido;
import edu.itm.agrotech.domain.Pedido;
import edu.itm.agrotech.dto.PedidoRequest;

import java.util.List;

/**
 * Contrato de negocio de Pedido. Los controladores dependen de esta
 * interfaz y no de la implementacion.
 */
public interface PedidoService {

    /**
     * Registra una venta: valida el cliente y las existencias, congela el
     * precio, calcula el total, descuenta el inventario y registra el pago
     * como PENDIENTE.
     *
     * @throws edu.itm.agrotech.exception.RecursoNoEncontradoException si el cliente o un producto no existen
     * @throws edu.itm.agrotech.exception.ReglaNegocioException si no hay existencias suficientes
     */
    Pedido registrarVenta(PedidoRequest solicitud);

    /** @throws edu.itm.agrotech.exception.RecursoNoEncontradoException si no existe */
    Pedido consultar(Long id);

    /**
     * Historial de compras de un cliente, del mas reciente al mas antiguo.
     *
     * @param estado filtro opcional; nulo devuelve todos los pedidos
     * @throws edu.itm.agrotech.exception.RecursoNoEncontradoException si el cliente no existe
     */
    List<Pedido> listarPorCliente(Long idCliente, EstadoPedido estado);

    /**
     * Mueve el pedido por su ciclo de vida y aplica los efectos de cada paso
     * sobre el pago y el inventario.
     *
     * @throws edu.itm.agrotech.exception.ReglaNegocioException si la transicion no esta permitida
     */
    Pedido cambiarEstado(Long id, EstadoPedido nuevoEstado);
}
