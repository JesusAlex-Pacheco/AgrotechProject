package edu.itm.agrotech.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Registro de un cliente")
public record RegistroClienteRequest(

        @Schema(description = "Datos de acceso")
        @NotNull(message = "Los datos del usuario son obligatorios")
        @Valid
        DatosUsuarioRequest usuario,

        @Schema(description = "Direccion por defecto para las entregas (opcional)",
                example = "Carrera 43A # 1-50, Medellin")
        @Size(max = 200, message = "La direccion no puede superar 200 caracteres")
        String direccion
) {
}
