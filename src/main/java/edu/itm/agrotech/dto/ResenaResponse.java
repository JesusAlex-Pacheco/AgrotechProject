package edu.itm.agrotech.dto;

import edu.itm.agrotech.entity.Resena;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Resena de un producto")
public record ResenaResponse(
        Long id,
        Long idProducto,
        Long idCliente,
        String nombreCliente,
        Integer calificacion,
        String comentario,
        LocalDate fecha
) {

    public static ResenaResponse desde(Resena resena) {
        return new ResenaResponse(
                resena.getId(),
                resena.getIdProducto(),
                resena.getCliente().getId(),
                resena.getCliente().getUsuario().getNombre(),
                resena.getCalificacion(),
                resena.getComentario(),
                resena.getFecha()
        );
    }
}
