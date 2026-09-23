package edu.itm.agrotech.dto;

import edu.itm.agrotech.domain.MetodoPago;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/** Solicitud de compra: es la interaccion de negocio del sistema. */
public record PedidoRequest(

        @NotNull(message = "El cliente es obligatorio")
        Long idCliente,

        @NotNull(message = "El metodo de pago es obligatorio")
        MetodoPago metodoPago,

        @DecimalMin(value = "0.0", message = "El costo de envio no puede ser negativo")
        BigDecimal costoEnvio,

        @NotEmpty(message = "El pedido debe tener al menos un producto")
        @Valid
        List<ItemPedidoRequest> items
) {
}
