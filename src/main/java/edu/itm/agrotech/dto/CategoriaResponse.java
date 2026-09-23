package edu.itm.agrotech.dto;

import edu.itm.agrotech.domain.Categoria;

/** Representacion de la categoria que se devuelve al cliente de la API. */
public record CategoriaResponse(
        Long id,
        String nombre
) {

    public static CategoriaResponse desde(Categoria categoria) {
        return new CategoriaResponse(categoria.getId(), categoria.getNombre());
    }
}
