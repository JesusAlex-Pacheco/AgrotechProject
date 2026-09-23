package edu.itm.agrotech.repository.jdbc;

import edu.itm.agrotech.domain.Producto;
import edu.itm.agrotech.exception.ErrorPersistenciaException;
import edu.itm.agrotech.repository.ProductoRepository;
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
 * Implementacion de la persistencia con JDBC puro: Connection,
 * PreparedStatement y sentencias SQL escritas a mano.
 */
@Repository
public class ProductoRepositoryJdbc implements ProductoRepository {

    private static final String SQL_INSERTAR = """
            INSERT INTO producto
                (id_agricultor, id_categoria, nombre, descripcion, precio,
                 unidad_medida, stock, disponible, foto_url)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String SQL_BUSCAR_POR_ID = """
            SELECT id_producto, id_agricultor, id_categoria, nombre, descripcion,
                   precio, unidad_medida, stock, disponible, foto_url
            FROM producto
            WHERE id_producto = ?
            """;

    private static final String SQL_ACTUALIZAR = """
            UPDATE producto
            SET id_categoria = ?, nombre = ?, descripcion = ?, precio = ?,
                unidad_medida = ?, stock = ?, disponible = ?, foto_url = ?
            WHERE id_producto = ?
            """;

    private static final String SQL_DESACTIVAR = """
            UPDATE producto
            SET disponible = FALSE
            WHERE id_producto = ?
            """;

    private final DataSource dataSource;

    public ProductoRepositoryJdbc(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Producto guardar(Producto producto) {
        try (Connection conexion = dataSource.getConnection();
             PreparedStatement sentencia =
                     conexion.prepareStatement(SQL_INSERTAR, Statement.RETURN_GENERATED_KEYS)) {

            sentencia.setLong(1, producto.getIdAgricultor());
            sentencia.setLong(2, producto.getIdCategoria());
            sentencia.setString(3, producto.getNombre());
            sentencia.setString(4, producto.getDescripcion());
            sentencia.setBigDecimal(5, producto.getPrecio());
            sentencia.setString(6, producto.getUnidadMedida());
            sentencia.setBigDecimal(7, producto.getStock());
            sentencia.setBoolean(8, producto.isDisponible());
            sentencia.setString(9, producto.getFotoUrl());

            sentencia.executeUpdate();

            try (ResultSet claves = sentencia.getGeneratedKeys()) {
                if (claves.next()) {
                    producto.setId(claves.getLong(1));
                }
            }
            return producto;

        } catch (SQLException e) {
            throw new ErrorPersistenciaException("No fue posible guardar el producto", e);
        }
    }

    @Override
    public Optional<Producto> buscarPorId(Long id) {
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
            throw new ErrorPersistenciaException("No fue posible consultar el producto", e);
        }
    }

    /**
     * Listado con filtros opcionales por categoria y por nombre.
     * La consulta se arma dinamicamente pero los valores siempre viajan
     * como parametros, para evitar inyeccion de SQL.
     */
    @Override
    public List<Producto> listar(Long idCategoria, String nombre) {
        StringBuilder sql = new StringBuilder("""
                SELECT id_producto, id_agricultor, id_categoria, nombre, descripcion,
                       precio, unidad_medida, stock, disponible, foto_url
                FROM producto
                WHERE 1 = 1
                """);

        List<Object> parametros = new ArrayList<>();

        if (idCategoria != null) {
            sql.append(" AND id_categoria = ? ");
            parametros.add(idCategoria);
        }
        if (nombre != null && !nombre.isBlank()) {
            sql.append(" AND LOWER(nombre) LIKE ? ");
            parametros.add("%" + nombre.toLowerCase() + "%");
        }
        sql.append(" ORDER BY nombre ");

        try (Connection conexion = dataSource.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(sql.toString())) {

            for (int i = 0; i < parametros.size(); i++) {
                sentencia.setObject(i + 1, parametros.get(i));
            }

            try (ResultSet filas = sentencia.executeQuery()) {
                List<Producto> productos = new ArrayList<>();
                while (filas.next()) {
                    productos.add(mapear(filas));
                }
                return productos;
            }

        } catch (SQLException e) {
            throw new ErrorPersistenciaException("No fue posible listar los productos", e);
        }
    }

    @Override
    public boolean actualizar(Producto producto) {
        try (Connection conexion = dataSource.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_ACTUALIZAR)) {

            sentencia.setLong(1, producto.getIdCategoria());
            sentencia.setString(2, producto.getNombre());
            sentencia.setString(3, producto.getDescripcion());
            sentencia.setBigDecimal(4, producto.getPrecio());
            sentencia.setString(5, producto.getUnidadMedida());
            sentencia.setBigDecimal(6, producto.getStock());
            sentencia.setBoolean(7, producto.isDisponible());
            sentencia.setString(8, producto.getFotoUrl());
            sentencia.setLong(9, producto.getId());

            return sentencia.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new ErrorPersistenciaException("No fue posible actualizar el producto", e);
        }
    }

    /**
     * Baja logica. No se elimina la fila porque el producto puede estar
     * referenciado en pedidos historicos.
     */
    @Override
    public boolean desactivar(Long id) {
        try (Connection conexion = dataSource.getConnection();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_DESACTIVAR)) {

            sentencia.setLong(1, id);
            return sentencia.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new ErrorPersistenciaException("No fue posible desactivar el producto", e);
        }
    }

    private Producto mapear(ResultSet fila) throws SQLException {
        Producto producto = new Producto();
        producto.setId(fila.getLong("id_producto"));
        producto.setIdAgricultor(fila.getLong("id_agricultor"));
        producto.setIdCategoria(fila.getLong("id_categoria"));
        producto.setNombre(fila.getString("nombre"));
        producto.setDescripcion(fila.getString("descripcion"));
        producto.setPrecio(fila.getBigDecimal("precio"));
        producto.setUnidadMedida(fila.getString("unidad_medida"));
        producto.setStock(fila.getBigDecimal("stock"));
        producto.setDisponible(fila.getBoolean("disponible"));
        producto.setFotoUrl(fila.getString("foto_url"));
        return producto;
    }
}
