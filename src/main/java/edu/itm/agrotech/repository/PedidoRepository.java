package edu.itm.agrotech.repository;

import edu.itm.agrotech.domain.Pedido;

import java.util.Optional;

public interface PedidoRepository {

    /**
     * Registra la venta completa como una unica transaccion:
     * inserta el pedido, descuenta el inventario, inserta las lineas
     * y registra el pago. Si algun paso falla, no se persiste nada.
     */
    Pedido registrarVenta(Pedido pedido);

    Optional<Pedido> buscarPorId(Long id);
}
