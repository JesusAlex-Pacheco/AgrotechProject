package edu.itm.agrotech.service.impl;

import edu.itm.agrotech.domain.DetallePedido;
import edu.itm.agrotech.domain.EstadoPedido;
import edu.itm.agrotech.domain.Pago;
import edu.itm.agrotech.domain.Pedido;
import edu.itm.agrotech.domain.Producto;
import edu.itm.agrotech.dto.ItemPedidoRequest;
import edu.itm.agrotech.dto.PedidoRequest;
import edu.itm.agrotech.exception.RecursoNoEncontradoException;
import edu.itm.agrotech.exception.ReglaNegocioException;
import edu.itm.agrotech.repository.ClienteRepository;
import edu.itm.agrotech.repository.PedidoRepository;
import edu.itm.agrotech.repository.ProductoRepository;
import edu.itm.agrotech.service.PedidoService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Interaccion de negocio del sistema: la venta directa del agricultor
 * al consumidor y el ciclo de vida del pedido hasta su entrega.
 */
@Service
public class PedidoServiceImpl implements PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;

    public PedidoServiceImpl(PedidoRepository pedidoRepository,
                             ProductoRepository productoRepository,
                             ClienteRepository clienteRepository) {
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
        this.clienteRepository = clienteRepository;
    }

    @Override
    public Pedido registrarVenta(PedidoRequest solicitud) {
        validarClienteExiste(solicitud.idCliente());

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
        pedido.setPago(Pago.pendiente(solicitud.metodoPago(), pedido.getTotal()));

        return pedidoRepository.registrarVenta(pedido);
    }

    @Override
    public Pedido consultar(Long id) {
        return pedidoRepository.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el pedido con id " + id));
    }

    @Override
    public List<Pedido> listarPorCliente(Long idCliente, EstadoPedido estado) {
        validarClienteExiste(idCliente);
        return pedidoRepository.listarPorCliente(idCliente, estado);
    }

    /**
     * Efectos de cada paso:
     * <ul>
     *   <li>PAGADO: el pago queda APROBADO.</li>
     *   <li>ENTREGADO contra entrega: el pago se cobra y queda APROBADO.</li>
     *   <li>CANCELADO: se devuelve el inventario y un pago PENDIENTE queda
     *       RECHAZADO. Un pago ya APROBADO no se toca: el reembolso se
     *       gestiona fuera del sistema.</li>
     * </ul>
     */
    @Override
    public Pedido cambiarEstado(Long id, EstadoPedido nuevoEstado) {
        Pedido pedido = consultar(id);
        EstadoPedido estadoActual = pedido.getEstado();
        Pago pago = pedido.getPago();

        validarTransicion(estadoActual, nuevoEstado, pago);
        aplicarEfectosSobrePago(nuevoEstado, pago);

        pedido.setEstado(nuevoEstado);
        pedidoRepository.actualizarEstado(
                pedido, estadoActual, nuevoEstado == EstadoPedido.CANCELADO);
        return pedido;
    }

    private void validarTransicion(EstadoPedido actual, EstadoPedido nuevo, Pago pago) {
        if (!actual.puedeCambiarA(nuevo)) {
            throw new ReglaNegocioException(
                    "No se puede pasar un pedido de " + actual + " a " + nuevo);
        }

        // Regla: con PSE o tarjeta, el pedido solo se despacha despues de pagado.
        if (actual == EstadoPedido.CREADO && nuevo == EstadoPedido.EN_RUTA
                && !pago.esContraEntrega()) {
            throw new ReglaNegocioException("Un pedido pagado con " + pago.getMetodo()
                    + " debe estar PAGADO antes de despacharse");
        }

        // Regla: un pedido contra entrega se cobra al entregarlo, no antes.
        if (nuevo == EstadoPedido.PAGADO && pago.esContraEntrega()) {
            throw new ReglaNegocioException(
                    "Un pedido contra entrega se paga al entregarse: pasa de CREADO a EN_RUTA");
        }
    }

    private void aplicarEfectosSobrePago(EstadoPedido nuevoEstado, Pago pago) {
        switch (nuevoEstado) {
            case PAGADO -> pago.aprobar(LocalDateTime.now());
            case ENTREGADO -> {
                if (pago.estaPendiente()) {
                    pago.aprobar(LocalDateTime.now());
                }
            }
            case CANCELADO -> {
                if (pago.estaPendiente()) {
                    pago.rechazar();
                }
            }
            default -> { }
        }
    }

    private void validarClienteExiste(Long idCliente) {
        if (!clienteRepository.existe(idCliente)) {
            throw new RecursoNoEncontradoException("No existe el cliente con id " + idCliente);
        }
    }
}
