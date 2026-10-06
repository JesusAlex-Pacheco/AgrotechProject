package edu.itm.agrotech.repository.jdbc;

import edu.itm.agrotech.domain.Categoria;
import edu.itm.agrotech.repository.CategoriaRepository;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
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

    private static final String SQL_TIENE_PRODUCTOS = """
            SELECT 1
            FROM producto
            WHERE id_categoria = ?
            LIMIT 1
            """;

    private final EjecutorJdbc jdbc;

    public CategoriaRepositoryJdbc(EjecutorJdbc jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Categoria guardar(Categoria categoria) {
        long id = jdbc.insertar(SQL_INSERTAR,
                sentencia -> sentencia.setString(1, categoria.getNombre()),
                "No fue posible guardar la categoria");

        categoria.setId(id);
        return categoria;
    }

    @Override
    public Optional<Categoria> buscarPorId(Long id) {
        return jdbc.consultarUno(SQL_BUSCAR_POR_ID,
                sentencia -> sentencia.setLong(1, id),
                this::mapear,
                "No fue posible consultar la categoria");
    }

    @Override
    public Optional<Categoria> buscarPorNombre(String nombre) {
        return jdbc.consultarUno(SQL_BUSCAR_POR_NOMBRE,
                sentencia -> sentencia.setString(1, nombre),
                this::mapear,
                "No fue posible consultar la categoria");
    }

    @Override
    public List<Categoria> listar() {
        return jdbc.consultar(SQL_LISTAR, EjecutorJdbc.SIN_PARAMETROS, this::mapear,
                "No fue posible listar las categorias");
    }

    @Override
    public boolean actualizar(Categoria categoria) {
        return jdbc.actualizar(SQL_ACTUALIZAR, sentencia -> {
            sentencia.setString(1, categoria.getNombre());
            sentencia.setLong(2, categoria.getId());
        }, "No fue posible actualizar la categoria") > 0;
    }

    @Override
    public boolean eliminar(Long id) {
        return jdbc.actualizar(SQL_ELIMINAR,
                sentencia -> sentencia.setLong(1, id),
                "No fue posible eliminar la categoria") > 0;
    }

    @Override
    public boolean tieneProductos(Long id) {
        return jdbc.consultarUno(SQL_TIENE_PRODUCTOS,
                sentencia -> sentencia.setLong(1, id),
                fila -> fila.getInt(1),
                "No fue posible verificar los productos de la categoria").isPresent();
    }

    private Categoria mapear(ResultSet fila) throws SQLException {
        Categoria categoria = new Categoria();
        categoria.setId(fila.getLong("id_categoria"));
        categoria.setNombre(fila.getString("nombre"));
        return categoria;
    }
}
