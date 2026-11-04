package edu.itm.agrotech.entity;

import edu.itm.agrotech.domain.EstadoEntrega;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

/**
 * Entidad JPA de la tabla entrega: la logistica de un pedido.
 * El pedido se persiste con JDBC, por eso aqui se guarda solo su id y no una
 * relacion JPA: cada tecnologia es duena de sus tablas.
 */
@Entity
@Table(name = "entrega")
@Getter
@Setter
@NoArgsConstructor
public class Entrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_entrega")
    private Long id;

    @Column(name = "id_pedido", nullable = false, unique = true)
    private Long idPedido;

    // Muchos a uno: un transportador atiende muchas entregas.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_transportador")
    private Transportador transportador;

    @Column(name = "direccion", nullable = false, length = 200)
    private String direccion;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoEntrega estado;

    @Column(name = "fecha_programada")
    private LocalDate fechaProgramada;

    @Column(name = "fecha_entrega")
    private LocalDate fechaEntrega;

    public static Entrega programar(Long idPedido, Transportador transportador,
                                    String direccion, LocalDate fechaProgramada) {
        Entrega entrega = new Entrega();
        entrega.idPedido = idPedido;
        entrega.transportador = transportador;
        entrega.direccion = direccion;
        entrega.fechaProgramada = fechaProgramada;
        entrega.estado = EstadoEntrega.PROGRAMADA;
        return entrega;
    }

    public void despachar() {
        this.estado = EstadoEntrega.EN_RUTA;
    }

    public void confirmar(LocalDate fecha) {
        this.estado = EstadoEntrega.ENTREGADA;
        this.fechaEntrega = fecha;
    }

    /** El pedido se cancelo antes de salir: la entrega ya no se realizara. */
    public void anular() {
        this.estado = EstadoEntrega.FALLIDA;
    }
}
