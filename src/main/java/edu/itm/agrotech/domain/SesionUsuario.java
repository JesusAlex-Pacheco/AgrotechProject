package edu.itm.agrotech.domain;

/**
 * Usuario que inicio sesion. idPerfil es el id de su perfil segun el rol
 * (agricultor, cliente o transportador); es null para ADMIN, que no tiene perfil.
 */
public record SesionUsuario(
        Long idUsuario,
        String nombre,
        String correo,
        Rol rol,
        Long idPerfil
) {
}
