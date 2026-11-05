package edu.itm.agrotech.entity;

import edu.itm.agrotech.domain.Rol;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Entidad JPA de la tabla usuario: los datos de acceso comunes a todos los
 * roles. Agricultor, Cliente y Transportador la referencian con @OneToOne.
 */
@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long id;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "correo", nullable = false, unique = true, length = 120)
    private String correo;

    /** Hash BCrypt de la clave. La clave en texto plano nunca se guarda. */
    @Column(name = "clave_hash", nullable = false)
    private String claveHash;

    @Column(name = "telefono", length = 20)
    private String telefono;

    // La columna es VARCHAR: sin @JdbcTypeCode, Hibernate esperaria un ENUM
    // nativo de MySQL y la validacion del esquema fallaria.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "rol", nullable = false, length = 20)
    private Rol rol;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @PrePersist
    void alRegistrar() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
    }
}
