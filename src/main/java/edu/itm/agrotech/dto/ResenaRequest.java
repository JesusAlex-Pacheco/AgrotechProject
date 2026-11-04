package edu.itm.agrotech.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Resena de un producto que el cliente ya recibio")
public record ResenaRequest(

        @Schema(description = "Cliente que califica", example = "1")
        @NotNull(message = "El cliente es obligatorio")
        Long idCliente,

        @Schema(description = "Calificacion de 1 a 5", example = "5")
        @NotNull(message = "La calificacion es obligatoria")
        @Min(value = 1, message = "La calificacion minima es 1")
        @Max(value = 5, message = "La calificacion maxima es 5")
        Integer calificacion,

        @Schema(description = "Comentario (opcional)", example = "Llego fresco y bien empacado")
        @Size(max = 500, message = "El comentario no puede superar 500 caracteres")
        String comentario
) {
}
