package edu.itm.agrotech.dto;

import edu.itm.agrotech.entity.Resena;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Schema(description = "Resenas de un producto con su calificacion promedio")
public record ResenasProductoResponse(

        Long idProducto,

        @Schema(description = "Promedio con un decimal. Nulo si aun no tiene resenas", example = "4.5")
        BigDecimal promedio,

        int total,

        List<ResenaResponse> resenas
) {

    public static ResenasProductoResponse desde(Long idProducto, List<Resena> resenas) {
        BigDecimal promedio = resenas.isEmpty() ? null : BigDecimal.valueOf(
                        resenas.stream().mapToInt(Resena::getCalificacion).average().orElseThrow())
                .setScale(1, RoundingMode.HALF_UP);

        return new ResenasProductoResponse(
                idProducto,
                promedio,
                resenas.size(),
                resenas.stream().map(ResenaResponse::desde).toList()
        );
    }
}
