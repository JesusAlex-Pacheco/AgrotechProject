package edu.itm.agrotech.security;

import edu.itm.agrotech.domain.Rol;

import java.util.Objects;

/**
 * Usuario que hace la solicitud, tal como viene en su token. idPerfil es el
 * id de su agricultor, cliente o transportador; es null para ADMIN.
 */
public record UsuarioActual(Long idUsuario, Rol rol, Long idPerfil) {

    public boolean esAdmin() {
        return rol == Rol.ADMIN;
    }

    /** Indica si es el perfil indicado del rol indicado, por ejemplo el cliente 3. */
    public boolean es(Rol rolEsperado, Long idPerfilEsperado) {
        return rol == rolEsperado && idPerfil != null && Objects.equals(idPerfil, idPerfilEsperado);
    }
}
