package edu.itm.agrotech.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Registro de un transportador")
public record RegistroTransportadorRequest(

        @Schema(description = "Datos de acceso")
        @NotNull(message = "Los datos del usuario son obligatorios")
        @Valid
        DatosUsuarioRequest usuario,

        @Schema(description = "Placa del vehiculo", example = "ABC123")
        @NotBlank(message = "La placa es obligatoria")
        @Size(max = 10, message = "La placa no puede superar 10 caracteres")
        String placa,

        @Schema(description = "Tipo de vehiculo (opcional)", example = "Camioneta")
        @Size(max = 40, message = "El tipo de vehiculo no puede superar 40 caracteres")
        String tipoVehiculo
) {
}
