package edu.itm.agrotech.repository.jdbc;

import edu.itm.agrotech.domain.Categoria;
import edu.itm.agrotech.exception.ErrorPersistenciaException;
import edu.itm.agrotech.repository.CategoriaRepository;
import org.springframework.stereotype.Repository;

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
 * Implementacion de la persistencia de Categoria con JDBC puro.
 */
@Repository
public class CategoriaRepositoryJdbc implements CategoriaRepository {

    private static final String SQL_INSERTAR = """
            INSERT INTO categoria (nombre)
            VALUES (?)
            """;

    private static final String SQL_BUSCAR_POR_ID = """
            SELECT id_categoria, nombre
            FROM categoria
            WHERE id_categoria = ?
            """;

    // La columna usa la intercalacion utf8mb4_unicode_ci, asi que la
    // comparacion no distingue mayusculas: "frutas" encuentra "Frutas".
    private static final String SQL_BUSCAR_POR_NOMBRE = """
            SELECT id_categoria, nombre
            FROM categoria
            WHERE nombre = ?
            """;

    private static final String SQL_LISTAR = """
            SELECT id_categoria, nombre
            FROM categoria
            ORDER BY nombre
            """;

    private static final String SQL_ACTUALIZAR = """
            UPDATE categoria
            SET nombre = ?
            WHERE id_categoria = ?
            """;

    private static final String SQL_ELIMINAR = """
            DELETE FROM categoria
            WHERE id_categoria = ?
            """;

    private static final String SQL_CONTAR_PRODUCTOS = """
            SELECT COUNT(*)
            FROM producto
            WHERE id_categoria = ?
            """;

    private final DataSource dataSource;

    public CategoriaRepositoryJdbc(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Categoria guardar(Categoria categoria) {
        try (Connection conexion = dataSource.getConnection();
             PreparedStatement sentencia =
                     conexion.prepareStatement(SQL_INSERTAR, Statement.RETURN_GENERATED_KEYS)) {

            sentencia.setString(1, categoria.getNombre());
            sentencia.executeUpdate();

            try (ResultSet claves = sentencia.getGeneratedKeys()) {
                if (claves.next()) {
                    categoria.setId(claves.getLong(1));
                }
            }
            return categoria;

        } catch (SQLException e) {
            throw new ErrorPersistenciaException("No fue posible guardar la categoria", e);
        }
    }

    @Override
    public Optional<Categoria> buscarPorId(Long id) {
        try (Connection conexion = dataSource.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_BUSCAR_POR_ID)) {

            sentencia.setLong(1, id);

            try (ResultSet fila = sentencia.executeQuery()) {
                if (fila.next()) {
                    return Optional.of(mapear(fila));
                }
                return Optional.empty();
            }

        } catch (SQLException e) {
            throw new ErrorPersistenciaException("No fue posible consultar la categoria", e);
        }
    }

    @Override
    public Optional<Categoria> buscarPorNombre(String nombre) {
        try (Connection conexion = dataSource.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_BUSCAR_POR_NOMBRE)) {

            sentencia.setString(1, nombre);

            try (ResultSet fila = sentencia.executeQuery()) {
                if (fila.next()) {
                    return Optional.of(mapear(fila));
                }
                return Optional.empty();
            }

        } catch (SQLException e) {
            throw new ErrorPersistenciaException("No fue posible consultar la categoria", e);
        }
    }

    @Override
    public List<Categoria> listar() {
        try (Connection conexion = dataSource.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_LISTAR);
             ResultSet filas = sentencia.executeQuery()) {

            List<Categoria> categorias = new ArrayList<>();
            while (filas.next()) {
                categorias.add(mapear(filas));
            }
            return categorias;

        } catch (SQLException e) {
            throw new ErrorPersistenciaException("No fue posible listar las categorias", e);
        }
    }

    @Override
    public boolean actualizar(Categoria categoria) {
        try (Connection conexion = dataSource.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_ACTUALIZAR)) {

            sentencia.setString(1, categoria.getNombre());
            sentencia.setLong(2, categoria.getId());
            return sentencia.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new ErrorPersistenciaException("No fue posible actualizar la categoria", e);
        }
    }

    @Override
    public boolean eliminar(Long id) {
        try (Connection conexion = dataSource.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_ELIMINAR)) {

            sentencia.setLong(1, id);
            return sentencia.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new ErrorPersistenciaException("No fue posible eliminar la categoria", e);
        }
    }

    @Override
    public boolean tieneProductos(Long id) {
        try (Connection conexion = dataSource.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_CONTAR_PRODUCTOS)) {

            sentencia.setLong(1, id);

            try (ResultSet fila = sentencia.executeQuery()) {
                return fila.next() && fila.getLong(1) > 0;
            }

        } catch (SQLException e) {
            throw new ErrorPersistenciaException("No fue posible verificar los productos de la categoria", e);
        }
    }

    private Categoria mapear(ResultSet fila) throws SQLException {
        Categoria categoria = new Categoria();
        categoria.setId(fila.getLong("id_categoria"));
        categoria.setNombre(fila.getString("nombre"));
        return categoria;
    }
}
