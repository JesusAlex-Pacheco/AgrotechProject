package edu.itm.agrotech.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Registro de un agricultor")
public record RegistroAgricultorRequest(

        @Schema(description = "Datos de acceso")
        @NotNull(message = "Los datos del usuario son obligatorios")
        @Valid
        DatosUsuarioRequest usuario,

        @Schema(description = "Nombre de la finca", example = "Finca El Roble")
        @NotBlank(message = "El nombre de la finca es obligatorio")
        @Size(max = 120, message = "El nombre de la finca no puede superar 120 caracteres")
        String nombreFinca,

        @Schema(description = "Municipio", example = "Rionegro")
        @NotBlank(message = "El municipio es obligatorio")
        @Size(max = 80, message = "El municipio no puede superar 80 caracteres")
        String municipio,

        @Schema(description = "Vereda (opcional)", example = "Llanogrande")
        @Size(max = 80, message = "La vereda no puede superar 80 caracteres")
        String vereda
) {
}
