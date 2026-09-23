package edu.itm.agrotech.service;

import edu.itm.agrotech.domain.DetallePedido;
import edu.itm.agrotech.domain.EstadoPago;
import edu.itm.agrotech.domain.EstadoPedido;
import edu.itm.agrotech.domain.MetodoPago;
import edu.itm.agrotech.domain.Pago;
import edu.itm.agrotech.domain.Pedido;
import edu.itm.agrotech.domain.Producto;
import edu.itm.agrotech.dto.ItemPedidoRequest;
import edu.itm.agrotech.dto.PedidoRequest;
import edu.itm.agrotech.exception.RecursoNoEncontradoException;
import edu.itm.agrotech.exception.ReglaNegocioException;
import edu.itm.agrotech.repository.PedidoRepository;
import edu.itm.agrotech.repository.ProductoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Interaccion de negocio del sistema: la venta directa del agricultor
 * al consumidor. Valida existencias, congela el precio, calcula el total,
 * descuenta el inventario y registra el pago.
 */
@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;

    public PedidoService(PedidoRepository pedidoRepository,
                         ProductoRepository productoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
    }

    public Pedido registrarVenta(PedidoRequest solicitud) {
        Pedido pedido = new Pedido();
        pedido.setIdCliente(solicitud.idCliente());
        pedido.setFecha(LocalDateTime.now());
        pedido.setCostoEnvio(
                solicitud.costoEnvio() == null ? BigDecimal.ZERO : solicitud.costoEnvio());

        for (ItemPedidoRequest item : solicitud.items()) {
            Producto producto = productoRepository.buscarPorId(item.idProducto())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "No existe el producto con id " + item.idProducto()));

            if (!producto.puedeVenderse(item.cantidad())) {
                throw new ReglaNegocioException(
                        "El producto '" + producto.getNombre()
                                + "' no tiene existencias suficientes. Disponible: "
                                + producto.getStock());
            }

            // El precio se congela en el momento de la compra: cambios
            // posteriores en el catalogo no alteran este pedido.
            pedido.agregarDetalle(new DetallePedido(
                    producto.getId(), item.cantidad(), producto.getPrecio()));
        }

        pedido.calcularTotal();
        pedido.setEstado(EstadoPedido.CREADO);
        pedido.setPago(construirPago(solicitud.metodoPago(), pedido.getTotal()));

        return pedidoRepository.registrarVenta(pedido);
    }

    public Pedido consultar(Long id) {
        return pedidoRepository.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el pedido con id " + id));
    }

    /**
     * El pago nace PENDIENTE: con PSE o tarjeta queda a la espera de la
     * confirmacion de la pasarela; contra entrega, del cobro al momento
     * de la entrega.
     */
    private Pago construirPago(MetodoPago metodo, BigDecimal total) {
        Pago pago = new Pago();
        pago.setMetodo(metodo);
        pago.setEstado(EstadoPago.PENDIENTE);
        pago.setMonto(total);
        pago.setReferenciaPasarela(null);
        pago.setFechaPago(null);
        return pago;
    }
}
