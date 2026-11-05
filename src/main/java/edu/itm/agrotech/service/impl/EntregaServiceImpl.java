package edu.itm.agrotech.service.impl;

import edu.itm.agrotech.domain.EstadoEntrega;
import edu.itm.agrotech.domain.EstadoPedido;
import edu.itm.agrotech.domain.Pedido;
import edu.itm.agrotech.dto.EntregaRequest;
import edu.itm.agrotech.entity.Cliente;
import edu.itm.agrotech.entity.Entrega;
import edu.itm.agrotech.entity.Transportador;
import edu.itm.agrotech.exception.RecursoNoEncontradoException;
import edu.itm.agrotech.exception.ReglaNegocioException;
import edu.itm.agrotech.repository.ClienteRepository;
import edu.itm.agrotech.repository.EntregaRepository;
import edu.itm.agrotech.repository.PedidoRepository;
import edu.itm.agrotech.repository.TransportadorRepository;
import edu.itm.agrotech.service.EntregaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Logistica de entrega. Combina las dos persistencias del proyecto: lee el
 * pedido por JDBC y guarda la entrega por JPA. Solo escribe en tablas JPA,
 * asi que su transaccion nunca mezcla las dos tecnologias.
 */
@Service
public class EntregaServiceImpl implements EntregaService {

    private final EntregaRepository entregaRepository;
    private final TransportadorRepository transportadorRepository;
    private final ClienteRepository clienteRepository;
    private final PedidoRepository pedidoRepository;

    public EntregaServiceImpl(EntregaRepository entregaRepository,
                              TransportadorRepository transportadorRepository,
                              ClienteRepository clienteRepository,
                              PedidoRepository pedidoRepository) {
        this.entregaRepository = entregaRepository;
        this.transportadorRepository = transportadorRepository;
        this.clienteRepository = clienteRepository;
        this.pedidoRepository = pedidoRepository;
    }

    @Override
    @Transactional
    public Entrega programar(Long idPedido, EntregaRequest solicitud) {
        Pedido pedido = pedidoRepository.buscarPorId(idPedido)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el pedido con id " + idPedido));

        validarPedidoSinDespachar(pedido);
        if (entregaRepository.existsByIdPedido(idPedido)) {
            throw new ReglaNegocioException("El pedido " + idPedido + " ya tiene una entrega programada");
        }

        Transportador transportador = transportadorRepository.buscarConUsuario(solicitud.idTransportador())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el transportador con id " + solicitud.idTransportador()));

        String direccion = resolverDireccion(solicitud.direccion(), pedido.getIdCliente());

        return entregaRepository.save(
                Entrega.programar(idPedido, transportador, direccion, solicitud.fechaProgramada()));
    }

    @Override
    @Transactional(readOnly = true)
    public Entrega consultarPorPedido(Long idPedido) {
        return entregaRepository.buscarPorPedido(idPedido)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El pedido " + idPedido + " no tiene una entrega programada"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Entrega> listarPorTransportador(Long idTransportador, EstadoEntrega estado) {
        if (!transportadorRepository.existsById(idTransportador)) {
            throw new RecursoNoEncontradoException("No existe el transportador con id " + idTransportador);
        }
        return entregaRepository.listarPorTransportador(idTransportador, estado);
    }

    /** Regla: solo se programa la entrega de un pedido que aun no ha salido. */
    private void validarPedidoSinDespachar(Pedido pedido) {
        EstadoPedido estado = pedido.getEstado();
        if (estado != EstadoPedido.CREADO && estado != EstadoPedido.PAGADO) {
            throw new ReglaNegocioException("Solo se programa la entrega de un pedido CREADO o PAGADO; "
                    + "el pedido " + pedido.getId() + " esta " + estado);
        }
    }

    /** La direccion de la solicitud tiene prioridad; si no viene, se usa la del cliente. */
    private String resolverDireccion(String direccionSolicitada, Long idCliente) {
        if (direccionSolicitada != null && !direccionSolicitada.isBlank()) {
            return direccionSolicitada.trim();
        }

        return clienteRepository.findById(idCliente)
                .map(Cliente::getDireccion)
                .filter(direccion -> direccion != null && !direccion.isBlank())
                .orElseThrow(() -> new ReglaNegocioException(
                        "El cliente no tiene direccion registrada: indique la direccion de entrega"));
    }
}
