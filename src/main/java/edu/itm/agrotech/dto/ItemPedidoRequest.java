package edu.itm.agrotech.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Linea de un pedido: un producto y la cantidad que se compra")
public record ItemPedidoRequest(

        @Schema(example = "1")
        @NotNull(message = "El producto es obligatorio")
        Long idProducto,

        @Schema(description = "Cantidad en la unidad de medida del producto", example = "3")
        @NotNull(message = "La cantidad es obligatoria")
        @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor que cero")
        BigDecimal cantidad
) {
}
