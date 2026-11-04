package edu.itm.agrotech.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "Programacion de la entrega de un pedido")
public record EntregaRequest(

        @Schema(description = "Transportador asignado", example = "1")
        @NotNull(message = "El transportador es obligatorio")
        Long idTransportador,

        @Schema(description = "Direccion de entrega. Si no se envia, se usa la direccion del cliente",
                example = "Calle 50 # 40-20, Itagui")
        @Size(max = 200, message = "La direccion no puede superar 200 caracteres")
        String direccion,

        @Schema(description = "Fecha en que se hara la entrega (hoy o despues)", example = "2026-12-01")
        @NotNull(message = "La fecha programada es obligatoria")
        @FutureOrPresent(message = "La fecha programada no puede estar en el pasado")
        LocalDate fechaProgramada
) {
}
