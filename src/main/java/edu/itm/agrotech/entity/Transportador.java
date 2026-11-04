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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** Entidad JPA de la tabla transportador: quien lleva los pedidos al cliente. */
@Entity
@Table(name = "transportador")
@Getter
@Setter
@NoArgsConstructor
public class Transportador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_transportador")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "id_usuario", nullable = false, unique = true)
    private Usuario usuario;

    @Column(name = "placa", nullable = false, length = 10)
    private String placa;

    @Column(name = "tipo_vehiculo", length = 40)
    private String tipoVehiculo;

    // Uno a muchos: lado inverso de Entrega.transportador.
    @OneToMany(mappedBy = "transportador")
    private List<Entrega> entregas = new ArrayList<>();
}
