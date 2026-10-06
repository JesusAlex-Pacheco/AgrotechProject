package edu.itm.agrotech.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;

/** Formato unico de todas las respuestas de error de la API. */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Respuesta de error de la API")
public record ErrorResponse(

        @Schema(description = "Momento del error", example = "2026-10-03T10:15:30")
        LocalDateTime fecha,

        @Schema(description = "Codigo HTTP", example = "404")
        int estado,

        @Schema(description = "Explicacion del error", example = "No existe el producto con id 999")
        String mensaje,

        @Schema(description = "Errores por campo. Solo aparece en errores de validacion (400)",
                example = "{\"nombre\": \"El nombre es obligatorio\"}")
        Map<String, String> errores
) {

    public static ErrorResponse de(HttpStatus estado, String mensaje) {
        return new ErrorResponse(LocalDateTime.now(), estado.value(), mensaje, null);
    }

    public static ErrorResponse deValidacion(Map<String, String> errores) {
        return new ErrorResponse(LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(),
                "Datos invalidos", errores);
    }
}
