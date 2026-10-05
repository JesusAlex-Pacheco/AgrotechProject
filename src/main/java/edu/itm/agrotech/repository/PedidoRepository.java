package edu.itm.agrotech.repository;

import edu.itm.agrotech.domain.EstadoPedido;
import edu.itm.agrotech.domain.Pedido;

import java.util.List;
import java.util.Optional;

public interface PedidoRepository {

    /**
     * Registra la venta completa como una unica transaccion:
     * inserta el pedido, descuenta el inventario, inserta las lineas
     * y registra el pago. Si algun paso falla, no se persiste nada.
     */
    Pedido registrarVenta(Pedido pedido);

    Optional<Pedido> buscarPorId(Long id);

    /**
     * Pedidos de un cliente, del mas reciente al mas antiguo, con sus lineas
     * y su pago. Si {@code estado} no es nulo, solo los que esten en ese estado.
     */
    List<Pedido> listarPorCliente(Long idCliente, EstadoPedido estado);

    /**
     * Guarda el nuevo estado del pedido y de su pago como una unica
     * transaccion. Si {@code devolverStock} es verdadero, suma de nuevo al
     * inventario las cantidades de cada linea. Si otro proceso cambio el
     * estado del pedido desde {@code estadoAnterior}, no se persiste nada.
     */
    void actualizarEstado(Pedido pedido, EstadoPedido estadoAnterior, boolean devolverStock);
}
