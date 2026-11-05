package edu.itm.agrotech.service;

import edu.itm.agrotech.domain.EstadoEntrega;
import edu.itm.agrotech.dto.EntregaRequest;
import edu.itm.agrotech.entity.Entrega;

import java.util.List;

/** Contrato de negocio de la logistica de entrega de los pedidos. */
public interface EntregaService {

    /**
     * Programa la entrega de un pedido aun no despachado y le asigna un
     * transportador. Sin direccion en la solicitud, usa la del cliente.
     *
     * @throws edu.itm.agrotech.exception.RecursoNoEncontradoException si el pedido o el transportador no existen
     * @throws edu.itm.agrotech.exception.ReglaNegocioException si el pedido ya salio, ya tiene entrega
     *         o no hay direccion a donde llevarlo
     */
    Entrega programar(Long idPedido, EntregaRequest solicitud);

    /** @throws edu.itm.agrotech.exception.RecursoNoEncontradoException si el pedido no tiene entrega */
    Entrega consultarPorPedido(Long idPedido);

    /**
     * Agenda del transportador, por fecha programada.
     *
     * @param estado filtro opcional; nulo devuelve todas
     * @throws edu.itm.agrotech.exception.RecursoNoEncontradoException si el transportador no existe
     */
    List<Entrega> listarPorTransportador(Long idTransportador, EstadoEntrega estado);
}
