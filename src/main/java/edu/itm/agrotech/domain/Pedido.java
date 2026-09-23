package edu.itm.agrotech.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class Pedido {

    private Long id;
    private Long idCliente;
    private LocalDateTime fecha;
    private EstadoPedido estado;
    private BigDecimal subtotal = BigDecimal.ZERO;
    private BigDecimal costoEnvio = BigDecimal.ZERO;
    private BigDecimal total = BigDecimal.ZERO;
    private List<DetallePedido> detalles = new ArrayList<>();
    private Pago pago;

    /** Regla de dominio: el total se calcula a partir de las lineas del pedido. */
    public void calcularTotal() {
        this.subtotal = detalles.stream()
                .map(DetallePedido::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.total = this.subtotal.add(costoEnvio == null ? BigDecimal.ZERO : costoEnvio);
    }

    public void agregarDetalle(DetallePedido detalle) {
        this.detalles.add(detalle);
    }

}
