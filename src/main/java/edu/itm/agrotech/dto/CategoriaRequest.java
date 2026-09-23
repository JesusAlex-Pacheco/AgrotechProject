package edu.itm.agrotech.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Datos que llegan desde el front end para crear o actualizar una categoria. */
public record CategoriaRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 60, message = "El nombre no puede superar 60 caracteres")
        String nombre
) {
}
