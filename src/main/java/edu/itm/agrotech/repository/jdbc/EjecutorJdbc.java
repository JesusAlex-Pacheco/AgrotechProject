package edu.itm.agrotech.repository.jdbc;

import edu.itm.agrotech.exception.ErrorPersistenciaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Plantilla minima sobre JDBC. Concentra el patron que antes se repetia en
 * cada metodo de los repositorios: pedir la conexion, preparar la sentencia,
 * asignar los parametros, ejecutar, cerrar los recursos y traducir la
 * SQLException. Cada repositorio solo escribe su SQL, sus parametros y su
 * mapeo de filas.
 */
@Component
public class EjecutorJdbc {

    private static final Logger LOG = LoggerFactory.getLogger(EjecutorJdbc.class);

    /** Asigna los parametros (?) de una sentencia preparada. */
    @FunctionalInterface
    interface Parametros {
        void asignar(PreparedStatement sentencia) throws SQLException;
    }

    /** Convierte la fila actual de un ResultSet en un objeto. */
    @FunctionalInterface
    interface MapeadorFila<T> {
        T mapear(ResultSet fila) throws SQLException;
    }

    /** Trabajo que se ejecuta dentro de una transaccion, sobre una sola conexion. */
    @FunctionalInterface
    interface Transaccion<T> {
        T ejecutar(Connection conexion) throws SQLException;
    }

    static final Parametros SIN_PARAMETROS = sentencia -> { };

    private final DataSource dataSource;

    public EjecutorJdbc(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Ejecuta un SELECT y mapea cada fila del resultado. */
    <T> List<T> consultar(String sql, Parametros parametros, MapeadorFila<T> mapeador,
                          String mensajeError) {
        try (Connection conexion = dataSource.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql)) {

            parametros.asignar(sentencia);

            try (ResultSet filas = sentencia.executeQuery()) {
                List<T> resultado = new ArrayList<>();
                while (filas.next()) {
                    resultado.add(mapeador.mapear(filas));
                }
                return resultado;
            }

        } catch (SQLException e) {
            throw new ErrorPersistenciaException(mensajeError, e);
        }
    }

    /** Ejecuta un SELECT que devuelve como maximo una fila. */
    <T> Optional<T> consultarUno(String sql, Parametros parametros, MapeadorFila<T> mapeador,
                                 String mensajeError) {
        return consultar(sql, parametros, mapeador, mensajeError).stream().findFirst();
    }

    /** Ejecuta un UPDATE o DELETE con su propia conexion. Devuelve las filas afectadas. */
    int actualizar(String sql, Parametros parametros, String mensajeError) {
        try (Connection conexion = dataSource.getConnection()) {
            return actualizarEn(conexion, sql, parametros);
        } catch (SQLException e) {
            throw new ErrorPersistenciaException(mensajeError, e);
        }
    }

    /** Ejecuta un INSERT con su propia conexion. Devuelve la clave generada. */
    long insertar(String sql, Parametros parametros, String mensajeError) {
        try (Connection conexion = dataSource.getConnection()) {
            return insertarEn(conexion, sql, parametros);
        } catch (SQLException e) {
            throw new ErrorPersistenciaException(mensajeError, e);
        }
    }

    /**
     * Unidad de trabajo: todas las sentencias del trabajo comparten una
     * conexion con autocommit desactivado. Si el trabajo termina, se
     * confirma; si lanza cualquier excepcion, se revierte todo y la
     * excepcion se propaga (las reglas de negocio conservan su tipo).
     */
    <T> T enTransaccion(Transaccion<T> trabajo, String mensajeError) {
        Connection conexion = null;
        try {
            conexion = dataSource.getConnection();
            conexion.setAutoCommit(false);

            T resultado = trabajo.ejecutar(conexion);

            conexion.commit();
            return resultado;

        } catch (SQLException e) {
            revertir(conexion, e);
            throw new ErrorPersistenciaException(mensajeError, e);
        } catch (RuntimeException e) {
            revertir(conexion, e);
            throw e;
        } finally {
            cerrar(conexion);
        }
    }

    /** UPDATE o DELETE sobre una conexion existente, para usar dentro de una transaccion. */
    static int actualizarEn(Connection conexion, String sql, Parametros parametros)
            throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(sql)) {
            parametros.asignar(sentencia);
            return sentencia.executeUpdate();
        }
    }

    /** INSERT sobre una conexion existente. Devuelve la clave AUTO_INCREMENT generada. */
    static long insertarEn(Connection conexion, String sql, Parametros parametros)
            throws SQLException {
        try (PreparedStatement sentencia =
                     conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            parametros.asignar(sentencia);
            sentencia.executeUpdate();

            try (ResultSet claves = sentencia.getGeneratedKeys()) {
                if (claves.next()) {
                    return claves.getLong(1);
                }
            }
            throw new SQLException("La base de datos no devolvio la clave generada");
        }
    }

    private void revertir(Connection conexion, Exception causa) {
        if (conexion == null) {
            return;
        }
        try {
            conexion.rollback();
        } catch (SQLException e) {
            // Se adjunta al error original para no ocultar la causa real.
            causa.addSuppressed(e);
        }
    }

    private void cerrar(Connection conexion) {
        if (conexion == null) {
            return;
        }
        try {
            conexion.setAutoCommit(true);
            conexion.close();
        } catch (SQLException e) {
            LOG.warn("No fue posible cerrar la conexion", e);
        }
    }
}
