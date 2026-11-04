package edu.itm.agrotech.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Datos de acceso comunes a todos los roles. Se reutiliza en cada registro. */
@Schema(description = "Datos de acceso del usuario")
public record DatosUsuarioRequest(

        @Schema(description = "Nombre completo", example = "Marta Lucia Ospina")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
        String nombre,

        @Schema(description = "Correo unico, sin importar mayusculas", example = "marta@agrotech.co")
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no tiene un formato valido")
        @Size(max = 120, message = "El correo no puede superar 120 caracteres")
        String correo,

        @Schema(description = "Clave de acceso. Se guarda cifrada con BCrypt", example = "Campo2026*")
        @NotBlank(message = "La clave es obligatoria")
        @Size(min = 8, max = 72, message = "La clave debe tener entre 8 y 72 caracteres")
        String clave,

        @Schema(description = "Telefono de contacto", example = "3107778899")
        @Size(max = 20, message = "El telefono no puede superar 20 caracteres")
        String telefono
) {
}
