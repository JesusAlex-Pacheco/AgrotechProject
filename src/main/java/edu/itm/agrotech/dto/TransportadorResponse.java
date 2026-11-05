package edu.itm.agrotech.dto;

import edu.itm.agrotech.entity.Transportador;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Transportador registrado")
public record TransportadorResponse(
        Long id,
        UsuarioResponse usuario,
        String placa,
        String tipoVehiculo
) {

    public static TransportadorResponse desde(Transportador transportador) {
        return new TransportadorResponse(
                transportador.getId(),
                UsuarioResponse.desde(transportador.getUsuario()),
                transportador.getPlaca(),
                transportador.getTipoVehiculo()
        );
    }
}
