package edu.itm.agrotech.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Entidad del dominio. No conoce la base de datos: la persistencia
 * es responsabilidad de la capa de repositorios.
 */
@Getter
@Setter
@NoArgsConstructor
public class Producto {

    private Long id;
    private Long idAgricultor;
    private Long idCategoria;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private String unidadMedida;
    private BigDecimal stock;
    private boolean disponible;
    private String fotoUrl;

    /** Regla de dominio: solo se puede vender si esta disponible y tiene existencias. */
    public boolean puedeVenderse(BigDecimal cantidad) {
        return disponible
                && stock != null
                && cantidad != null
                && stock.compareTo(cantidad) >= 0;
    }

}
