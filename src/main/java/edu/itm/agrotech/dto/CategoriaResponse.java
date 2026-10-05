package edu.itm.agrotech.dto;

import edu.itm.agrotech.domain.Categoria;
import io.swagger.v3.oas.annotations.media.Schema;

/** Representacion de la categoria que se devuelve al cliente de la API. */
@Schema(description = "Categoria de productos")
public record CategoriaResponse(
        Long id,
        String nombre
) {

    public static CategoriaResponse desde(Categoria categoria) {
        return new CategoriaResponse(categoria.getId(), categoria.getNombre());
    }
}
