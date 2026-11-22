package edu.itm.agrotech.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Credenciales para iniciar sesion. */
@Schema(description = "Credenciales de acceso")
public record InicioSesionRequest(
        @Schema(description = "Correo con el que se registro el usuario", example = "marta@agrotech.co")
        @NotBlank(message = "El correo es obligatorio")
        String correo,
        @Schema(description = "Clave de acceso", example = "Campo2026*")
        @NotBlank(message = "La clave es obligatoria")
        String clave
) {
}
