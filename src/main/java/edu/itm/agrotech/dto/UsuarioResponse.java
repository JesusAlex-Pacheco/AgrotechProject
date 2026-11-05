package edu.itm.agrotech.dto;

import edu.itm.agrotech.entity.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/** Datos publicos del usuario. Nunca incluye la clave ni su hash. */
@Schema(description = "Datos del usuario, sin la clave")
public record UsuarioResponse(
        Long id,
        String nombre,
        String correo,
        String telefono,
        String rol,
        LocalDateTime fechaRegistro
) {

    public static UsuarioResponse desde(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getCorreo(),
                usuario.getTelefono(),
                usuario.getRol().name(),
                usuario.getFechaRegistro()
        );
    }
}
