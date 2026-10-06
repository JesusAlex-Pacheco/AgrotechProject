package edu.itm.agrotech.repository.jdbc;

import edu.itm.agrotech.domain.DetallePedido;
import edu.itm.agrotech.domain.EstadoPago;
import edu.itm.agrotech.domain.EstadoPedido;
import edu.itm.agrotech.domain.MetodoPago;
import edu.itm.agrotech.domain.Pago;
import edu.itm.agrotech.domain.Pedido;
import edu.itm.agrotech.exception.ReglaNegocioException;
import edu.itm.agrotech.repository.PedidoRepository;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class PedidoRepositoryJdbc implements PedidoRepository {

    private static final String SQL_INSERTAR_PEDIDO = """
            INSERT INTO pedido (id_cliente, fecha, estado, subtotal, costo_envio, total)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

    // Descuento condicional: si otro pedido consumio las existencias entre la
    // validacion y este punto, la sentencia no afecta ninguna fila.
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

    private static final String COLUMNAS_PEDIDO =
            "id_pedido, id_cliente, fecha, estado, subtotal, costo_envio, total";

    private static final String SQL_BUSCAR_PEDIDO =
            "SELECT " + COLUMNAS_PEDIDO + " FROM pedido WHERE id_pedido = ?";

    private static final String SQL_LISTAR_POR_CLIENTE =
            "SELECT " + COLUMNAS_PEDIDO + " FROM pedido WHERE id_cliente = ?";

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

    // Solo cambia el estado si sigue siendo el que se valido en la capa de
    // negocio. Si otro proceso lo cambio antes, no afecta ninguna fila.
    private static final String SQL_ACTUALIZAR_ESTADO = """
            UPDATE pedido
            SET estado = ?
            WHERE id_pedido = ? AND estado = ?
            """;

    private static final String SQL_DEVOLVER_STOCK = """
            UPDATE producto
            SET stock = stock + ?
            WHERE id_producto = ?
            """;

    private static final String SQL_ACTUALIZAR_PAGO = """
            UPDATE pago
            SET estado = ?, fecha_pago = ?
            WHERE id_pedido = ?
            """;

    private final EjecutorJdbc jdbc;

    public PedidoRepositoryJdbc(EjecutorJdbc jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Pedido registrarVenta(Pedido pedido) {
        return jdbc.enTransaccion(conexion -> {
            insertarPedido(conexion, pedido);

            for (DetallePedido detalle : pedido.getDetalles()) {
                descontarStock(conexion, detalle);
                insertarDetalle(conexion, pedido.getId(), detalle);
            }

            insertarPago(conexion, pedido);
            return pedido;
        }, "No fue posible registrar el pedido");
    }

    @Override
    public void actualizarEstado(Pedido pedido, EstadoPedido estadoAnterior, boolean devolverStock) {
        jdbc.enTransaccion(conexion -> {
            guardarEstado(conexion, pedido, estadoAnterior);

            if (devolverStock) {
                for (DetallePedido detalle : pedido.getDetalles()) {
                    devolverStock(conexion, detalle);
                }
            }

            if (pedido.getPago() != null) {
                actualizarPago(conexion, pedido.getId(), pedido.getPago());
            }
            return null;
        }, "No fue posible actualizar el estado del pedido");
    }

    @Override
    public Optional<Pedido> buscarPorId(Long id) {
        return jdbc.consultarUno(SQL_BUSCAR_PEDIDO,
                        sentencia -> sentencia.setLong(1, id),
                        this::mapearPedido,
                        "No fue posible consultar el pedido")
                .map(this::cargarDetallesYPago);
    }

    /**
     * Carga primero los pedidos y luego, por cada uno, sus lineas y su pago.
     * Para el volumen de un historial por cliente es suficiente y mantiene
     * el mapeo igual al de la consulta individual.
     */
    @Override
    public List<Pedido> listarPorCliente(Long idCliente, EstadoPedido estado) {
        String sql = SQL_LISTAR_POR_CLIENTE
                + (estado != null ? " AND estado = ?" : "")
                + " ORDER BY fecha DESC, id_pedido DESC";

        List<Pedido> pedidos = jdbc.consultar(sql, sentencia -> {
            sentencia.setLong(1, idCliente);
            if (estado != null) {
                sentencia.setString(2, estado.name());
            }
        }, this::mapearPedido, "No fue posible listar los pedidos del cliente");

        pedidos.forEach(this::cargarDetallesYPago);
        return pedidos;
    }

    // ---------------------------------------------------------------
    // Escritura (siempre dentro de una transaccion)
    // ---------------------------------------------------------------

    private void insertarPedido(Connection conexion, Pedido pedido) throws SQLException {
        long id = EjecutorJdbc.insertarEn(conexion, SQL_INSERTAR_PEDIDO, sentencia -> {
            sentencia.setLong(1, pedido.getIdCliente());
            sentencia.setTimestamp(2, Timestamp.valueOf(pedido.getFecha()));
            sentencia.setString(3, pedido.getEstado().name());
            sentencia.setBigDecimal(4, pedido.getSubtotal());
            sentencia.setBigDecimal(5, pedido.getCostoEnvio());
            sentencia.setBigDecimal(6, pedido.getTotal());
        });
        pedido.setId(id);
    }

    private void descontarStock(Connection conexion, DetallePedido detalle) throws SQLException {
        int filas = EjecutorJdbc.actualizarEn(conexion, SQL_DESCONTAR_STOCK, sentencia -> {
            sentencia.setBigDecimal(1, detalle.getCantidad());
            sentencia.setLong(2, detalle.getIdProducto());
            sentencia.setBigDecimal(3, detalle.getCantidad());
        });

        if (filas == 0) {
            throw new ReglaNegocioException(
                    "No hay existencias suficientes del producto " + detalle.getIdProducto());
        }
    }

    private void insertarDetalle(Connection conexion, Long idPedido, DetallePedido detalle)
            throws SQLException {
        long id = EjecutorJdbc.insertarEn(conexion, SQL_INSERTAR_DETALLE, sentencia -> {
            sentencia.setLong(1, idPedido);
            sentencia.setLong(2, detalle.getIdProducto());
            sentencia.setBigDecimal(3, detalle.getCantidad());
            sentencia.setBigDecimal(4, detalle.getPrecioUnitario());
            sentencia.setBigDecimal(5, detalle.getSubtotal());
        });
        detalle.setId(id);
        detalle.setIdPedido(idPedido);
    }

    private void insertarPago(Connection conexion, Pedido pedido) throws SQLException {
        Pago pago = pedido.getPago();
        long id = EjecutorJdbc.insertarEn(conexion, SQL_INSERTAR_PAGO, sentencia -> {
            sentencia.setLong(1, pedido.getId());
            sentencia.setString(2, pago.getMetodo().name());
            sentencia.setString(3, pago.getEstado().name());
            sentencia.setString(4, pago.getReferenciaPasarela());
            sentencia.setBigDecimal(5, pago.getMonto());
            asignarFecha(sentencia, 6, pago.getFechaPago());
        });
        pago.setId(id);
        pago.setIdPedido(pedido.getId());
    }

    private void guardarEstado(Connection conexion, Pedido pedido, EstadoPedido estadoAnterior)
            throws SQLException {
        int filas = EjecutorJdbc.actualizarEn(conexion, SQL_ACTUALIZAR_ESTADO, sentencia -> {
            sentencia.setString(1, pedido.getEstado().name());
            sentencia.setLong(2, pedido.getId());
            sentencia.setString(3, estadoAnterior.name());
        });

        if (filas == 0) {
            throw new ReglaNegocioException("El pedido " + pedido.getId()
                    + " cambio de estado mientras se procesaba la solicitud");
        }
    }

    private void devolverStock(Connection conexion, DetallePedido detalle) throws SQLException {
        EjecutorJdbc.actualizarEn(conexion, SQL_DEVOLVER_STOCK, sentencia -> {
            sentencia.setBigDecimal(1, detalle.getCantidad());
            sentencia.setLong(2, detalle.getIdProducto());
        });
    }

    private void actualizarPago(Connection conexion, Long idPedido, Pago pago) throws SQLException {
        EjecutorJdbc.actualizarEn(conexion, SQL_ACTUALIZAR_PAGO, sentencia -> {
            sentencia.setString(1, pago.getEstado().name());
            asignarFecha(sentencia, 2, pago.getFechaPago());
            sentencia.setLong(3, idPedido);
        });
    }

    private static void asignarFecha(PreparedStatement sentencia, int posicion, LocalDateTime fecha)
            throws SQLException {
        if (fecha == null) {
            sentencia.setNull(posicion, Types.TIMESTAMP);
        } else {
            sentencia.setTimestamp(posicion, Timestamp.valueOf(fecha));
        }
    }

    // ---------------------------------------------------------------
    // Lectura
    // ---------------------------------------------------------------

    private Pedido cargarDetallesYPago(Pedido pedido) {
        jdbc.consultar(SQL_BUSCAR_DETALLES,
                        sentencia -> sentencia.setLong(1, pedido.getId()),
                        this::mapearDetalle,
                        "No fue posible consultar las lineas del pedido")
                .forEach(pedido::agregarDetalle);

        jdbc.consultarUno(SQL_BUSCAR_PAGO,
                        sentencia -> sentencia.setLong(1, pedido.getId()),
                        this::mapearPago,
                        "No fue posible consultar el pago del pedido")
                .ifPresent(pedido::setPago);

        return pedido;
    }

    private Pedido mapearPedido(ResultSet fila) throws SQLException {
        Pedido pedido = new Pedido();
        pedido.setId(fila.getLong("id_pedido"));
        pedido.setIdCliente(fila.getLong("id_cliente"));
        pedido.setFecha(fila.getTimestamp("fecha").toLocalDateTime());
        pedido.setEstado(EstadoPedido.valueOf(fila.getString("estado")));
        pedido.setSubtotal(fila.getBigDecimal("subtotal"));
        pedido.setCostoEnvio(fila.getBigDecimal("costo_envio"));
        pedido.setTotal(fila.getBigDecimal("total"));
        return pedido;
    }

    private DetallePedido mapearDetalle(ResultSet fila) throws SQLException {
        DetallePedido detalle = new DetallePedido();
        detalle.setId(fila.getLong("id_detalle"));
        detalle.setIdPedido(fila.getLong("id_pedido"));
        detalle.setIdProducto(fila.getLong("id_producto"));
        detalle.setCantidad(fila.getBigDecimal("cantidad"));
        detalle.setPrecioUnitario(fila.getBigDecimal("precio_unitario"));
        detalle.setSubtotal(fila.getBigDecimal("subtotal"));
        return detalle;
    }

    private Pago mapearPago(ResultSet fila) throws SQLException {
        Pago pago = new Pago();
        pago.setId(fila.getLong("id_pago"));
        pago.setIdPedido(fila.getLong("id_pedido"));
        pago.setMetodo(MetodoPago.valueOf(fila.getString("metodo")));
        pago.setEstado(EstadoPago.valueOf(fila.getString("estado")));
        pago.setReferenciaPasarela(fila.getString("referencia_pasarela"));
        pago.setMonto(fila.getBigDecimal("monto"));
        Timestamp fechaPago = fila.getTimestamp("fecha_pago");
        pago.setFechaPago(fechaPago == null ? null : fechaPago.toLocalDateTime());
        return pago;
    }
}
