package edu.itm.agrotech.repository.jdbc;

import edu.itm.agrotech.domain.Producto;
import edu.itm.agrotech.repository.ProductoRepository;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementacion de la persistencia con JDBC puro: PreparedStatement y
 * sentencias SQL escritas a mano. El manejo de conexiones y errores lo
 * centraliza {@link EjecutorJdbc}.
 */
@Repository
public class ProductoRepositoryJdbc implements ProductoRepository {

    private static final String COLUMNAS = """
            id_producto, id_agricultor, id_categoria, nombre, descripcion,
            precio, unidad_medida, stock, disponible, foto_url
            """;

    private static final String SQL_INSERTAR = """
            INSERT INTO producto
                (id_agricultor, id_categoria, nombre, descripcion, precio,
                 unidad_medida, stock, disponible, foto_url)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String SQL_BUSCAR_POR_ID =
            "SELECT " + COLUMNAS + " FROM producto WHERE id_producto = ?";

    private static final String SQL_LISTAR =
            "SELECT " + COLUMNAS + " FROM producto WHERE 1 = 1";

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

    private final EjecutorJdbc jdbc;

    public ProductoRepositoryJdbc(EjecutorJdbc jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Producto guardar(Producto producto) {
        long id = jdbc.insertar(SQL_INSERTAR, sentencia -> {
            sentencia.setLong(1, producto.getIdAgricultor());
            sentencia.setLong(2, producto.getIdCategoria());
            sentencia.setString(3, producto.getNombre());
            sentencia.setString(4, producto.getDescripcion());
            sentencia.setBigDecimal(5, producto.getPrecio());
            sentencia.setString(6, producto.getUnidadMedida());
            sentencia.setBigDecimal(7, producto.getStock());
            sentencia.setBoolean(8, producto.isDisponible());
            sentencia.setString(9, producto.getFotoUrl());
        }, "No fue posible guardar el producto");

        producto.setId(id);
        return producto;
    }

    @Override
    public Optional<Producto> buscarPorId(Long id) {
        return jdbc.consultarUno(SQL_BUSCAR_POR_ID,
                sentencia -> sentencia.setLong(1, id),
                this::mapear,
                "No fue posible consultar el producto");
    }

    /**
     * Listado con filtros opcionales por categoria y por nombre.
     * La consulta se arma dinamicamente pero los valores siempre viajan
     * como parametros, para evitar inyeccion de SQL.
     */
    @Override
    public List<Producto> listar(Long idCategoria, String nombre) {
        StringBuilder sql = new StringBuilder(SQL_LISTAR);
        List<Object> valores = new ArrayList<>();

        if (idCategoria != null) {
            sql.append(" AND id_categoria = ?");
            valores.add(idCategoria);
        }
        if (nombre != null && !nombre.isBlank()) {
            sql.append(" AND LOWER(nombre) LIKE ?");
            valores.add("%" + nombre.toLowerCase() + "%");
        }
        sql.append(" ORDER BY nombre");

        return jdbc.consultar(sql.toString(), sentencia -> {
            for (int i = 0; i < valores.size(); i++) {
                sentencia.setObject(i + 1, valores.get(i));
            }
        }, this::mapear, "No fue posible listar los productos");
    }

    @Override
    public boolean actualizar(Producto producto) {
        return jdbc.actualizar(SQL_ACTUALIZAR, sentencia -> {
            sentencia.setLong(1, producto.getIdCategoria());
            sentencia.setString(2, producto.getNombre());
            sentencia.setString(3, producto.getDescripcion());
            sentencia.setBigDecimal(4, producto.getPrecio());
            sentencia.setString(5, producto.getUnidadMedida());
            sentencia.setBigDecimal(6, producto.getStock());
            sentencia.setBoolean(7, producto.isDisponible());
            sentencia.setString(8, producto.getFotoUrl());
            sentencia.setLong(9, producto.getId());
        }, "No fue posible actualizar el producto") > 0;
    }

    /**
     * Baja logica. No se elimina la fila porque el producto puede estar
     * referenciado en pedidos historicos.
     */
    @Override
    public boolean desactivar(Long id) {
        return jdbc.actualizar(SQL_DESACTIVAR,
                sentencia -> sentencia.setLong(1, id),
                "No fue posible desactivar el producto") > 0;
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
