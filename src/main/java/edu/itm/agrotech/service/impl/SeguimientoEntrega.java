package edu.itm.agrotech.service.impl;

import edu.itm.agrotech.domain.EstadoPedido;
import edu.itm.agrotech.domain.Pedido;
import edu.itm.agrotech.entity.Entrega;
import edu.itm.agrotech.exception.ReglaNegocioException;
import edu.itm.agrotech.repository.EntregaRepository;
import edu.itm.agrotech.service.SeguimientoPedido;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Mantiene la entrega al ritmo del pedido:
 * <ul>
 *   <li>Antes de despachar (EN_RUTA) exige que la entrega este programada.</li>
 *   <li>Despues de cada cambio, la entrega pasa a EN_RUTA, ENTREGADA (con
 *       fecha) o FALLIDA si el pedido se cancelo.</li>
 * </ul>
 * La actualizacion de la entrega ocurre en su propia transaccion JPA, una
 * vez confirmada la transaccion JDBC del pedido.
 */
@Component
public class SeguimientoEntrega implements SeguimientoPedido {

    private final EntregaRepository entregaRepository;

    public SeguimientoEntrega(EntregaRepository entregaRepository) {
        this.entregaRepository = entregaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public void validarCambio(Pedido pedido, EstadoPedido nuevoEstado) {
        if (nuevoEstado == EstadoPedido.EN_RUTA && !entregaRepository.existsByIdPedido(pedido.getId())) {
            throw new ReglaNegocioException("Programe la entrega del pedido " + pedido.getId()
                    + " antes de despacharlo: POST /api/pedidos/" + pedido.getId() + "/entrega");
        }
    }

    @Override
    @Transactional
    public void registrarCambio(Pedido pedido, EstadoPedido nuevoEstado) {
        Optional<Entrega> encontrada = entregaRepository.findByIdPedido(pedido.getId());
        if (encontrada.isEmpty()) {
            return;
        }

        Entrega entrega = encontrada.get();
        switch (nuevoEstado) {
            case EN_RUTA -> entrega.despachar();
            case ENTREGADO -> entrega.confirmar(LocalDate.now());
            case CANCELADO -> entrega.anular();
            default -> {
                return;
            }
        }
        entregaRepository.save(entrega);
    }
}
