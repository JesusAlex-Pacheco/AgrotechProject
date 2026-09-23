package edu.itm.agrotech.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Agrupa los productos del catalogo: Frutas, Verduras, Tuberculos...
 */
@Getter
@Setter
@NoArgsConstructor
public class Categoria {

    private Long id;
    private String nombre;

}
