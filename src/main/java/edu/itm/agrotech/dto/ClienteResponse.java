package edu.itm.agrotech.dto;

import edu.itm.agrotech.entity.Cliente;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Cliente registrado")
public record ClienteResponse(
        Long id,
        UsuarioResponse usuario,
        String direccion
) {

    public static ClienteResponse desde(Cliente cliente) {
        return new ClienteResponse(
                cliente.getId(),
                UsuarioResponse.desde(cliente.getUsuario()),
                cliente.getDireccion()
        );
    }
}
