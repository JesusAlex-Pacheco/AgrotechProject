package edu.itm.agrotech.dto;

import edu.itm.agrotech.entity.Entrega;
import edu.itm.agrotech.entity.Transportador;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Entrega de un pedido con su transportador")
public record EntregaResponse(
        Long id,
        Long idPedido,
        Long idTransportador,
        String nombreTransportador,
        String placa,
        String direccion,
        String estado,
        LocalDate fechaProgramada,
        LocalDate fechaEntrega
) {

    public static EntregaResponse desde(Entrega entrega) {
        Transportador transportador = entrega.getTransportador();
        return new EntregaResponse(
                entrega.getId(),
                entrega.getIdPedido(),
                transportador == null ? null : transportador.getId(),
                transportador == null ? null : transportador.getUsuario().getNombre(),
                transportador == null ? null : transportador.getPlaca(),
                entrega.getDireccion(),
                entrega.getEstado().name(),
                entrega.getFechaProgramada(),
                entrega.getFechaEntrega()
        );
    }
}
