package edu.itm.agrotech.dto;

import edu.itm.agrotech.entity.Agricultor;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Agricultor registrado")
public record AgricultorResponse(
        Long id,
        UsuarioResponse usuario,
        String nombreFinca,
        String municipio,
        String vereda
) {

    public static AgricultorResponse desde(Agricultor agricultor) {
        return new AgricultorResponse(
                agricultor.getId(),
                UsuarioResponse.desde(agricultor.getUsuario()),
                agricultor.getNombreFinca(),
                agricultor.getMunicipio(),
                agricultor.getVereda()
        );
    }
}
