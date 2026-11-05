package edu.itm.agrotech.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Entidad JPA de la tabla agricultor: el productor que publica en el catalogo. */
@Entity
@Table(name = "agricultor")
@Getter
@Setter
@NoArgsConstructor
public class Agricultor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_agricultor")
    private Long id;

    // Uno a uno: cada agricultor es exactamente un usuario (id_usuario es
    // UNIQUE). PERSIST guarda el usuario nuevo junto con el agricultor.
    @OneToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "id_usuario", nullable = false, unique = true)
    private Usuario usuario;

    @Column(name = "nombre_finca", nullable = false, length = 120)
    private String nombreFinca;

    @Column(name = "municipio", nullable = false, length = 80)
    private String municipio;

    @Column(name = "vereda", length = 80)
    private String vereda;
}
