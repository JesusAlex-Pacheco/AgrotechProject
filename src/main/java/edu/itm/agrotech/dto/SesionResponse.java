package edu.itm.agrotech.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import edu.itm.agrotech.domain.SesionUsuario;
import edu.itm.agrotech.security.ServicioTokens.TokenEmitido;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/** Datos del usuario que inicio sesion y su token. Nunca incluye la clave ni su hash. */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Usuario que inicio sesion")
public record SesionResponse(
        @Schema(description = "Id del usuario", example = "1")
        Long idUsuario,
        @Schema(description = "Nombre completo", example = "Marta Lucia Ospina")
        String nombre,
        @Schema(description = "Correo", example = "marta@agrotech.co")
        String correo,
        @Schema(description = "Rol del usuario", example = "AGRICULTOR")
        String rol,
        @Schema(description = "Id del perfil segun el rol (agricultor, cliente o transportador). "
                + "No aparece para ADMIN", example = "1")
        Long idPerfil,
        @Schema(description = "Token de sesion. Se envia en cada solicitud como 'Authorization: Bearer <token>'")
        String token,
        @Schema(description = "Momento en que vence el token", example = "2026-10-03T18:15:30Z")
        Instant expira
) {
    public static SesionResponse desde(SesionUsuario sesion, TokenEmitido token) {
        return new SesionResponse(
                sesion.idUsuario(),
                sesion.nombre(),
                sesion.correo(),
                sesion.rol().name(),
                sesion.idPerfil(),
                token.valor(),
                token.expira()
        );
    }
}
