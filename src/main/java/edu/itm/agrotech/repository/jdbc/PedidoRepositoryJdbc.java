package edu.itm.agrotech.repository.jdbc;

import edu.itm.agrotech.domain.DetallePedido;
import edu.itm.agrotech.domain.EstadoPago;
import edu.itm.agrotech.domain.EstadoPedido;
import edu.itm.agrotech.domain.MetodoPago;
import edu.itm.agrotech.domain.Pago;
import edu.itm.agrotech.domain.Pedido;
import edu.itm.agrotech.exception.ErrorPersistenciaException;
import edu.itm.agrotech.exception.ReglaNegocioException;
import edu.itm.agrotech.repository.PedidoRepository;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Optional;

@Repository
public class PedidoRepositoryJdbc implements PedidoRepository {

    private static final String SQL_INSERTAR_PEDIDO = """
            INSERT INTO pedido (id_cliente, fecha, estado, subtotal, costo_envio, total)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

    private static final String SQL_DESCONTAR_STOCK = """
            UPDATE producto
            SET stock = stock - ?
            WHERE id_producto = ? AND disponible = TRUE AND stock >= ?
            """;

    private static final String SQL_INSERTAR_DETALLE = """
            INSERT INTO detalle_pedido (id_pedido, id_producto, cantidad, precio_unitario, subtotal)
            VALUES (?, ?, ?, ?, ?)
            """;

    private static final String SQL_INSERTAR_PAGO = """
            INSERT INTO pago (id_pedido, metodo, estado, referencia_pasarela, monto, fecha_pago)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

    private static final String SQL_BUSCAR_PEDIDO = """
            SELECT id_pedido, id_cliente, fecha, estado, subtotal, costo_envio, total
            FROM pedido
            WHERE id_pedido = ?
            """;

    private static final String SQL_BUSCAR_DETALLES = """
            SELECT id_detalle, id_pedido, id_producto, cantidad, precio_unitario, subtotal
            FROM detalle_pedido
            WHERE id_pedido = ?
            ORDER BY id_detalle
            """;

    private static final String SQL_BUSCAR_PAGO = """
            SELECT id_pago, id_pedido, metodo, estado, referencia_pasarela, monto, fecha_pago
            FROM pago
            WHERE id_pedido = ?
            """;

    private final DataSource dataSource;

    public PedidoRepositoryJdbc(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Unidad de trabajo: las cuatro operaciones comparten una misma conexion
     * con autocommit desactivado. O se confirman todas, o no se aplica ninguna.
     */
    @Override
    public Pedido registrarVenta(Pedido pedido) {
        Connection conexion = null;
        try {
            conexion = dataSource.getConnection();
            conexion.setAutoCommit(false);

            insertarPedido(conexion, pedido);

            for (DetallePedido detalle : pedido.getDetalles()) {
                descontarStock(conexion, detalle);
                insertarDetalle(conexion, pedido.getId(), detalle);
            }

            insertarPago(conexion, pedido);

            conexion.commit();
            return pedido;

        } catch (ReglaNegocioException e) {
            revertir(conexion);
            throw e;
        } catch (SQLException e) {
            revertir(conexion);
            throw new ErrorPersistenciaException("No fue posible registrar el pedido", e);
        } finally {
            cerrar(conexion);
        }
    }

    private void insertarPedido(Connection conexion, Pedido pedido) throws SQLException {
        try (PreparedStatement sentencia =
                     conexion.prepareStatement(SQL_INSERTAR_PEDIDO, Statement.RETURN_GENERATED_KEYS)) {

            sentencia.setLong(1, pedido.getIdCliente());
            sentencia.setTimestamp(2, Timestamp.valueOf(pedido.getFecha()));
            sentencia.setString(3, pedido.getEstado().name());
            sentencia.setBigDecimal(4, pedido.getSubtotal());
            sentencia.setBigDecimal(5, pedido.getCostoEnvio());
            sentencia.setBigDecimal(6, pedido.getTotal());

            sentencia.executeUpdate();

            try (ResultSet claves = sentencia.getGeneratedKeys()) {
                if (claves.next()) {
                    pedido.setId(claves.getLong(1));
                }
            }
        }
    }

    /**
     * El descuento se hace con una sentencia condicional. Si otro pedido
     * consumio las existencias entre la validacion y este punto, la sentencia
     * no afecta ninguna fila y la transaccion se revierte.
     */
    private void descontarStock(Connection conexion, DetallePedido detalle) throws SQLException {
        try (PreparedStatement sentencia = conexion.prepareStatement(SQL_DESCONTAR_STOCK)) {

            sentencia.setBigDecimal(1, detalle.getCantidad());
            sentencia.setLong(2, detalle.getIdProducto());
            sentencia.setBigDecimal(3, detalle.getCantidad());

            if (sentencia.executeUpdate() == 0) {
                throw new ReglaNegocioException(
                        "No hay existencias suficientes del producto " + detalle.getIdProducto());
            }
        }
    }

    private void insertarDetalle(Connection conexion, Long idPedido, DetallePedido detalle)
            throws SQLException {
        try (PreparedStatement sentencia =
                     conexion.prepareStatement(SQL_INSERTAR_DETALLE, Statement.RETURN_GENERATED_KEYS)) {

            sentencia.setLong(1, idPedido);
            sentencia.setLong(2, detalle.getIdProducto());
            sentencia.setBigDecimal(3, detalle.getCantidad());
            sentencia.setBigDecimal(4, detalle.getPrecioUnitario());
            sentencia.setBigDecimal(5, detalle.getSubtotal());

            sentencia.executeUpdate();
            detalle.setIdPedido(idPedido);

            try (ResultSet claves = sentencia.getGeneratedKeys()) {
                if (claves.next()) {
                    detalle.setId(claves.getLong(1));
                }
            }
        }
    }

    private void insertarPago(Connection conexion, Pedido pedido) throws SQLException {
        Pago pago = pedido.getPago();
        try (PreparedStatement sentencia =
                     conexion.prepareStatement(SQL_INSERTAR_PAGO, Statement.RETURN_GENERATED_KEYS)) {

            sentencia.setLong(1, pedido.getId());
            sentencia.setString(2, pago.getMetodo().name());
            sentencia.setString(3, pago.getEstado().name());
            sentencia.setString(4, pago.getReferenciaPasarela());
            sentencia.setBigDecimal(5, pago.getMonto());
            if (pago.getFechaPago() == null) {
                sentencia.setNull(6, java.sql.Types.TIMESTAMP);
            } else {
                sentencia.setTimestamp(6, Timestamp.valueOf(pago.getFechaPago()));
            }

            sentencia.executeUpdate();
            pago.setIdPedido(pedido.getId());

            try (ResultSet claves = sentencia.getGeneratedKeys()) {
                if (claves.next()) {
                    pago.setId(claves.getLong(1));
                }
            }
        }
    }

    @Override
    public Optional<Pedido> buscarPorId(Long id) {
        try (Connection conexion = dataSource.getConnection()) {

            Pedido pedido;
            try (PreparedStatement sentencia = conexion.prepareStatement(SQL_BUSCAR_PEDIDO)) {
                sentencia.setLong(1, id);
                try (ResultSet fila = sentencia.executeQuery()) {
                    if (!fila.next()) {
                        return Optional.empty();
                    }
                    pedido = new Pedido();
                    pedido.setId(fila.getLong("id_pedido"));
                    pedido.setIdCliente(fila.getLong("id_cliente"));
                    pedido.setFecha(fila.getTimestamp("fecha").toLocalDateTime());
                    pedido.setEstado(EstadoPedido.valueOf(fila.getString("estado")));
                    pedido.setSubtotal(fila.getBigDecimal("subtotal"));
                    pedido.setCostoEnvio(fila.getBigDecimal("costo_envio"));
                    pedido.setTotal(fila.getBigDecimal("total"));
                }
            }

            try (PreparedStatement sentencia = conexion.prepareStatement(SQL_BUSCAR_DETALLES)) {
                sentencia.setLong(1, id);
                try (ResultSet filas = sentencia.executeQuery()) {
                    while (filas.next()) {
                        DetallePedido detalle = new DetallePedido();
                        detalle.setId(filas.getLong("id_detalle"));
                        detalle.setIdPedido(filas.getLong("id_pedido"));
                        detalle.setIdProducto(filas.getLong("id_producto"));
                        detalle.setCantidad(filas.getBigDecimal("cantidad"));
                        detalle.setPrecioUnitario(filas.getBigDecimal("precio_unitario"));
                        detalle.setSubtotal(filas.getBigDecimal("subtotal"));
                        pedido.agregarDetalle(detalle);
                    }
                }
            }

            try (PreparedStatement sentencia = conexion.prepareStatement(SQL_BUSCAR_PAGO)) {
                sentencia.setLong(1, id);
                try (ResultSet fila = sentencia.executeQuery()) {
                    if (fila.next()) {
                        Pago pago = new Pago();
                        pago.setId(fila.getLong("id_pago"));
                        pago.setIdPedido(fila.getLong("id_pedido"));
                        pago.setMetodo(MetodoPago.valueOf(fila.getString("metodo")));
                        pago.setEstado(EstadoPago.valueOf(fila.getString("estado")));
                        pago.setReferenciaPasarela(fila.getString("referencia_pasarela"));
                        pago.setMonto(fila.getBigDecimal("monto"));
                        Timestamp fechaPago = fila.getTimestamp("fecha_pago");
                        pago.setFechaPago(fechaPago == null ? null : fechaPago.toLocalDateTime());
                        pedido.setPago(pago);
                    }
                }
            }

            return Optional.of(pedido);

        } catch (SQLException e) {
            throw new ErrorPersistenciaException("No fue posible consultar el pedido", e);
        }
    }

    private void revertir(Connection conexion) {
        if (conexion != null) {
            try {
                conexion.rollback();
            } catch (SQLException e) {
                throw new ErrorPersistenciaException("No fue posible revertir la transaccion", e);
            }
        }
    }

    private void cerrar(Connection conexion) {
        if (conexion != null) {
            try {
                conexion.setAutoCommit(true);
                conexion.close();
            } catch (SQLException e) {
                throw new ErrorPersistenciaException("No fue posible cerrar la conexion", e);
            }
        }
    }
}
