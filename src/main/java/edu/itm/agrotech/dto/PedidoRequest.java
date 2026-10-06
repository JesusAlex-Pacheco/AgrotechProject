package edu.itm.agrotech.dto;

import edu.itm.agrotech.domain.MetodoPago;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/** Solicitud de compra: es la interaccion de negocio del sistema. */
@Schema(description = "Solicitud de compra de uno o varios productos")
public record PedidoRequest(

        @Schema(description = "Cliente que compra", example = "1")
        @NotNull(message = "El cliente es obligatorio")
        Long idCliente,

        @Schema(description = "Forma de pago", example = "PSE")
        @NotNull(message = "El metodo de pago es obligatorio")
        MetodoPago metodoPago,

        @Schema(description = "Costo del envio. Si no se envia, vale 0", example = "6000.00")
        @DecimalMin(value = "0.0", message = "El costo de envio no puede ser negativo")
        BigDecimal costoEnvio,

        @Schema(description = "Productos y cantidades")
        @NotEmpty(message = "El pedido debe tener al menos un producto")
        @Valid
        List<ItemPedidoRequest> items
) {
}
