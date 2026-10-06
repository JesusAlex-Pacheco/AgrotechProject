package edu.itm.agrotech.domain;

public enum EstadoPedido {
    CREADO, PAGADO, EN_RUTA, ENTREGADO, CANCELADO;

    /**
     * Regla de dominio: transiciones permitidas del ciclo de vida.
     * <pre>
     * CREADO  -> PAGADO | EN_RUTA | CANCELADO
     * PAGADO  -> EN_RUTA | CANCELADO
     * EN_RUTA -> ENTREGADO
     * ENTREGADO y CANCELADO son estados finales.
     * </pre>
     * CREADO -> EN_RUTA solo aplica a pedidos contra entrega; esa condicion
     * depende del pago y la valida la capa de negocio.
     */
    public boolean puedeCambiarA(EstadoPedido nuevo) {
        return switch (this) {
            case CREADO -> nuevo == PAGADO || nuevo == EN_RUTA || nuevo == CANCELADO;
            case PAGADO -> nuevo == EN_RUTA || nuevo == CANCELADO;
            case EN_RUTA -> nuevo == ENTREGADO;
            case ENTREGADO, CANCELADO -> false;
        };
    }
}
