package edu.itm.agrotech.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** Entidad JPA de la tabla cliente: el consumidor que compra y resena. */
@Entity
@Table(name = "cliente")
@Getter
@Setter
@NoArgsConstructor
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "id_usuario", nullable = false, unique = true)
    private Usuario usuario;

    /** Direccion por defecto para las entregas. */
    @Column(name = "direccion", length = 200)
    private String direccion;

    // Uno a muchos: lado inverso de Resena.cliente. La llave foranea vive en
    // la tabla resena; esta lista solo la recorre, de la mas reciente a la
    // mas antigua.
    @OneToMany(mappedBy = "cliente")
    @OrderBy("fecha DESC, id DESC")
    private List<Resena> resenas = new ArrayList<>();

    /** Mantiene sincronizados los dos lados de la relacion. */
    public void agregarResena(Resena resena) {
        resenas.add(resena);
        resena.setCliente(this);
    }
}
