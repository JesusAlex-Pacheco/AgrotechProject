package edu.itm.agrotech.domain;

/**
 * Estado de la entrega fisica de un pedido. Coincide con ck_entrega_estado.
 * Avanza junto con el pedido: EN_RUTA al despacharlo, ENTREGADA al
 * entregarlo y FALLIDA si el pedido se cancela antes de salir.
 */
public enum EstadoEntrega {
    PROGRAMADA, EN_RUTA, ENTREGADA, FALLIDA
}
