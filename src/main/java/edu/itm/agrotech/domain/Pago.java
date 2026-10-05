package edu.itm.agrotech.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class Pago {

    private Long id;
    private Long idPedido;
    private MetodoPago metodo;
    private EstadoPago estado;
    private String referenciaPasarela;
    private BigDecimal monto;
    private LocalDateTime fechaPago;

    /**
     * Todo pago nace PENDIENTE: con PSE o tarjeta queda a la espera de la
     * confirmacion de la pasarela; contra entrega, del cobro al momento
     * de la entrega.
     */
    public static Pago pendiente(MetodoPago metodo, BigDecimal monto) {
        Pago pago = new Pago();
        pago.setMetodo(metodo);
        pago.setMonto(monto);
        pago.setEstado(EstadoPago.PENDIENTE);
        return pago;
    }

    /** Regla de dominio: un pago aprobado registra el momento del cobro. */
    public void aprobar(LocalDateTime fecha) {
        this.estado = EstadoPago.APROBADO;
        this.fechaPago = fecha;
    }

    public void rechazar() {
        this.estado = EstadoPago.RECHAZADO;
        this.fechaPago = null;
    }

    public boolean estaPendiente() {
        return estado == EstadoPago.PENDIENTE;
    }

    public boolean esContraEntrega() {
        return metodo == MetodoPago.CONTRA_ENTREGA;
    }
}
