package edu.itm.agrotech.dto;

import edu.itm.agrotech.domain.Producto;

import java.math.BigDecimal;

/** Representacion del producto que se devuelve al cliente de la API. */
public record ProductoResponse(
        Long id,
        Long idAgricultor,
        Long idCategoria,
        String nombre,
        String descripcion,
        BigDecimal precio,
        String unidadMedida,
        BigDecimal stock,
        boolean disponible,
        String fotoUrl
) {

    public static ProductoResponse desde(Producto producto) {
        return new ProductoResponse(
                producto.getId(),
                producto.getIdAgricultor(),
                producto.getIdCategoria(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getPrecio(),
                producto.getUnidadMedida(),
                producto.getStock(),
                producto.isDisponible(),
                producto.getFotoUrl()
        );
    }
}
