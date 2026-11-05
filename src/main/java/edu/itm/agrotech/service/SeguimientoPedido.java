package edu.itm.agrotech.service;

import edu.itm.agrotech.domain.EstadoPedido;
import edu.itm.agrotech.domain.Pedido;

/**
 * Punto de extension del ciclo de vida del pedido (patron observador).
 * PedidoServiceImpl avisa a todas las implementaciones sin conocerlas: para
 * reaccionar a un cambio de estado basta con agregar una nueva, sin
 * modificar el servicio de pedidos (principio abierto/cerrado).
 */
public interface SeguimientoPedido {

    /**
     * Se invoca antes de guardar el cambio, con el pedido aun en su estado actual.
     *
     * @throws edu.itm.agrotech.exception.ReglaNegocioException para impedir el cambio
     */
    void validarCambio(Pedido pedido, EstadoPedido nuevoEstado);

    /** Se invoca despues de que el nuevo estado del pedido quedo guardado. */
    void registrarCambio(Pedido pedido, EstadoPedido nuevoEstado);
}
