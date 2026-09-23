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

}
