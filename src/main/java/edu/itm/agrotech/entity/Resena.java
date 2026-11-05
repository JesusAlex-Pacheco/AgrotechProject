package edu.itm.agrotech.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.LocalDate;

/**
 * Entidad JPA de la tabla resena: la calificacion que un cliente da a un
 * producto que recibio. El producto se persiste con JDBC, por eso se guarda
 * solo su id.
 */
@Entity
@Table(name = "resena")
@Getter
@Setter
@NoArgsConstructor
public class Resena {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_resena")
    private Long id;

    // Muchos a uno: un cliente escribe muchas resenas. Es el lado dueno de
    // la relacion (tiene la llave foranea id_cliente).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @Column(name = "id_producto", nullable = false)
    private Long idProducto;

    @Column(name = "calificacion", nullable = false)
    private Integer calificacion;

    @Column(name = "comentario", length = 500)
    private String comentario;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    public static Resena nueva(Long idProducto, Integer calificacion, String comentario, LocalDate fecha) {
        Resena resena = new Resena();
        resena.idProducto = idProducto;
        resena.calificacion = calificacion;
        resena.comentario = comentario;
        resena.fecha = fecha;
        return resena;
    }
}
