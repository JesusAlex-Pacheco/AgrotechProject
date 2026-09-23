package edu.itm.agrotech.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Datos que llegan desde el front end para crear o actualizar un producto. */
public record ProductoRequest(

        @NotNull(message = "El agricultor es obligatorio")
        Long idAgricultor,

        @NotNull(message = "La categoria es obligatoria")
        Long idCategoria,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
        String nombre,

        @Size(max = 500, message = "La descripcion no puede superar 500 caracteres")
        String descripcion,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor que cero")
        BigDecimal precio,

        @NotBlank(message = "La unidad de medida es obligatoria")
        String unidadMedida,

        @NotNull(message = "El stock es obligatorio")
        @DecimalMin(value = "0.0", message = "El stock no puede ser negativo")
        BigDecimal stock,

        String fotoUrl
) {
}
