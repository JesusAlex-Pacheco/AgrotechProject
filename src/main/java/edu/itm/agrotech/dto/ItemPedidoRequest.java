package edu.itm.agrotech.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ItemPedidoRequest(

        @NotNull(message = "El producto es obligatorio")
        Long idProducto,

        @NotNull(message = "La cantidad es obligatoria")
        @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor que cero")
        BigDecimal cantidad
) {
}
