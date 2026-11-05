package edu.itm.agrotech.service.impl;

import edu.itm.agrotech.domain.Producto;
import edu.itm.agrotech.dto.ResenaRequest;
import edu.itm.agrotech.entity.Cliente;
import edu.itm.agrotech.entity.Resena;
import edu.itm.agrotech.exception.RecursoNoEncontradoException;
import edu.itm.agrotech.exception.ReglaNegocioException;
import edu.itm.agrotech.repository.ClienteRepository;
import edu.itm.agrotech.repository.PedidoRepository;
import edu.itm.agrotech.repository.ProductoRepository;
import edu.itm.agrotech.repository.ResenaRepository;
import edu.itm.agrotech.service.ResenaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Resenas de productos. El producto y las compras se consultan por JDBC; la
 * resena y el cliente se manejan con JPA.
 */
@Service
public class ResenaServiceImpl implements ResenaService {

    private final ResenaRepository resenaRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final PedidoRepository pedidoRepository;

    public ResenaServiceImpl(ResenaRepository resenaRepository,
                             ClienteRepository clienteRepository,
                             ProductoRepository productoRepository,
                             PedidoRepository pedidoRepository) {
        this.resenaRepository = resenaRepository;
        this.clienteRepository = clienteRepository;
        this.productoRepository = productoRepository;
        this.pedidoRepository = pedidoRepository;
    }

    @Override
    @Transactional
    public Resena registrar(Long idProducto, ResenaRequest solicitud) {
        Producto producto = buscarProducto(idProducto);
        Cliente cliente = clienteRepository.buscarConUsuario(solicitud.idCliente())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el cliente con id " + solicitud.idCliente()));

        // Regla: solo opina quien recibio el producto, para que las
        // calificaciones reflejen compras reales.
        if (!pedidoRepository.clienteRecibioProducto(cliente.getId(), idProducto)) {
            throw new ReglaNegocioException("Solo se puede resenar un producto ya recibido: el cliente "
                    + cliente.getId() + " no tiene pedidos ENTREGADOS con '" + producto.getNombre() + "'");
        }

        // Regla: una resena por cliente y producto (uq_resena_cliente_producto).
        if (resenaRepository.existsByClienteIdAndIdProducto(cliente.getId(), idProducto)) {
            throw new ReglaNegocioException("El cliente " + cliente.getId()
                    + " ya reseno el producto '" + producto.getNombre() + "'");
        }

        Resena resena = Resena.nueva(idProducto, solicitud.calificacion(),
                limpiar(solicitud.comentario()), LocalDate.now());
        cliente.agregarResena(resena);
        return resenaRepository.save(resena);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Resena> listarPorProducto(Long idProducto) {
        buscarProducto(idProducto);
        return resenaRepository.listarPorProducto(idProducto);
    }

    /** Recorre la relacion @OneToMany Cliente.resenas, ya ordenada por @OrderBy. */
    @Override
    @Transactional(readOnly = true)
    public List<Resena> listarPorCliente(Long idCliente) {
        Cliente cliente = clienteRepository.buscarConResenas(idCliente)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el cliente con id " + idCliente));
        return List.copyOf(cliente.getResenas());
    }

    private Producto buscarProducto(Long idProducto) {
        return productoRepository.buscarPorId(idProducto)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el producto con id " + idProducto));
    }

    private static String limpiar(String comentario) {
        return comentario == null || comentario.isBlank() ? null : comentario.trim();
    }
}
