package edu.itm.agrotech.dto;

import edu.itm.agrotech.domain.EstadoPedido;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Solicitud para mover un pedido a otro estado de su ciclo de vida. */
public record CambioEstadoRequest(

        @Schema(description = "Estado al que pasa el pedido", example = "PAGADO")
        @NotNull(message = "El estado es obligatorio")
        EstadoPedido estado
) {
}
