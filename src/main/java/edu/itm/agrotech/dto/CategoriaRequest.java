package edu.itm.agrotech.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Datos que llegan desde el front end para crear o actualizar una categoria. */
@Schema(description = "Datos para crear o renombrar una categoria")
public record CategoriaRequest(

        @Schema(description = "Nombre unico, sin importar mayusculas", example = "Granos")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 60, message = "El nombre no puede superar 60 caracteres")
        String nombre
) {
}
