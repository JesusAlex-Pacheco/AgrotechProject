package edu.itm.agrotech.dto;

import edu.itm.agrotech.domain.DetallePedido;
import edu.itm.agrotech.domain.Pedido;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Representacion del pedido que se devuelve al cliente de la API, con sus lineas y su pago. */
@Schema(description = "Pedido con sus lineas y el estado de su pago")
public record PedidoResponse(
        Long id,
        Long idCliente,
        LocalDateTime fecha,
        String estado,
        BigDecimal subtotal,
        BigDecimal costoEnvio,
        BigDecimal total,
        String metodoPago,
        String estadoPago,
        List<LineaResponse> items
) {

    public record LineaResponse(
            Long idProducto,
            BigDecimal cantidad,
            BigDecimal precioUnitario,
            BigDecimal subtotal
    ) {
        public static LineaResponse desde(DetallePedido detalle) {
            return new LineaResponse(
                    detalle.getIdProducto(),
                    detalle.getCantidad(),
                    detalle.getPrecioUnitario(),
                    detalle.getSubtotal()
            );
        }
    }

    public static PedidoResponse desde(Pedido pedido) {
        return new PedidoResponse(
                pedido.getId(),
                pedido.getIdCliente(),
                pedido.getFecha(),
                pedido.getEstado() == null ? null : pedido.getEstado().name(),
                pedido.getSubtotal(),
                pedido.getCostoEnvio(),
                pedido.getTotal(),
                pedido.getPago() == null ? null : pedido.getPago().getMetodo().name(),
                pedido.getPago() == null ? null : pedido.getPago().getEstado().name(),
                pedido.getDetalles().stream().map(LineaResponse::desde).toList()
        );
    }
}
