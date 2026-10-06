package edu.itm.agrotech.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Datos que llegan desde el front end para crear o actualizar un producto. */
@Schema(description = "Datos para publicar o actualizar un producto")
public record ProductoRequest(

        @Schema(description = "Agricultor que publica el producto. No cambia al actualizar", example = "1")
        @NotNull(message = "El agricultor es obligatorio")
        Long idAgricultor,

        @Schema(description = "Categoria del producto", example = "1")
        @NotNull(message = "La categoria es obligatoria")
        Long idCategoria,

        @Schema(example = "Mango de azucar")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
        String nombre,

        @Schema(example = "Cosecha de la semana")
        @Size(max = 500, message = "La descripcion no puede superar 500 caracteres")
        String descripcion,

        @Schema(description = "Precio por unidad de medida, en pesos", example = "4200.00")
        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor que cero")
        BigDecimal precio,

        @Schema(example = "kg")
        @NotBlank(message = "La unidad de medida es obligatoria")
        String unidadMedida,

        @Schema(description = "Cantidad disponible. Con 0 el producto queda no disponible", example = "50.00")
        @NotNull(message = "El stock es obligatorio")
        @DecimalMin(value = "0.0", message = "El stock no puede ser negativo")
        BigDecimal stock,

        @Schema(description = "Enlace a una foto del producto (opcional)")
        String fotoUrl
) {
}
