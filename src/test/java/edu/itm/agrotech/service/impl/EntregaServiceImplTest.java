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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntregaServiceImplTest {

    private static final LocalDate MANANA = LocalDate.now().plusDays(1);

    @Mock
    private EntregaRepository entregaRepository;
    @Mock
    private TransportadorRepository transportadorRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private PedidoRepository pedidoRepository;

    private EntregaServiceImpl servicio;

    @BeforeEach
    void preparar() {
        servicio = new EntregaServiceImpl(entregaRepository, transportadorRepository,
                clienteRepository, pedidoRepository);
    }

    @Nested
    @DisplayName("Programar")
    class Programar {

        @Test
        @DisplayName("Crea la entrega PROGRAMADA con la direccion de la solicitud")
        void conDireccionDeLaSolicitud() {
            Transportador transportador = transportador();
            when(pedidoRepository.buscarPorId(1L)).thenReturn(Optional.of(pedidoEn(EstadoPedido.PAGADO)));
            when(entregaRepository.existsByIdPedido(1L)).thenReturn(false);
            when(transportadorRepository.buscarConUsuario(1L)).thenReturn(Optional.of(transportador));
            when(entregaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Entrega entrega = servicio.programar(1L, new EntregaRequest(1L, " Calle 10 # 5-20 ", MANANA));

            assertThat(entrega.getEstado()).isEqualTo(EstadoEntrega.PROGRAMADA);
            assertThat(entrega.getDireccion()).isEqualTo("Calle 10 # 5-20");
            assertThat(entrega.getTransportador()).isSameAs(transportador);
            assertThat(entrega.getIdPedido()).isEqualTo(1L);
            assertThat(entrega.getFechaProgramada()).isEqualTo(MANANA);
        }

        @Test
        @DisplayName("Sin direccion en la solicitud usa la del cliente")
        void conDireccionDelCliente() {
            when(pedidoRepository.buscarPorId(1L)).thenReturn(Optional.of(pedidoEn(EstadoPedido.CREADO)));
            when(entregaRepository.existsByIdPedido(1L)).thenReturn(false);
            when(transportadorRepository.buscarConUsuario(1L)).thenReturn(Optional.of(transportador()));
            when(clienteRepository.findById(7L)).thenReturn(Optional.of(clienteCon("Calle 50 # 40-20")));
            when(entregaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Entrega entrega = servicio.programar(1L, new EntregaRequest(1L, null, MANANA));

            assertThat(entrega.getDireccion()).isEqualTo("Calle 50 # 40-20");
        }

        @Test
        @DisplayName("Sin direccion en la solicitud ni en el cliente, lo rechaza")
        void sinDireccion() {
            when(pedidoRepository.buscarPorId(1L)).thenReturn(Optional.of(pedidoEn(EstadoPedido.CREADO)));
            when(entregaRepository.existsByIdPedido(1L)).thenReturn(false);
            when(transportadorRepository.buscarConUsuario(1L)).thenReturn(Optional.of(transportador()));
            when(clienteRepository.findById(7L)).thenReturn(Optional.of(clienteCon(null)));

            assertThatThrownBy(() -> servicio.programar(1L, new EntregaRequest(1L, "  ", MANANA)))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("no tiene direccion registrada");
            verify(entregaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Un pedido inexistente lanza 404")
        void pedidoInexistente() {
            when(pedidoRepository.buscarPorId(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.programar(99L, new EntregaRequest(1L, null, MANANA)))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessage("No existe el pedido con id 99");
        }

        @ParameterizedTest(name = "pedido {0}")
        @EnumSource(value = EstadoPedido.class, names = {"EN_RUTA", "ENTREGADO", "CANCELADO"})
        @DisplayName("Solo acepta pedidos que aun no han salido")
        void pedidoYaDespachado(EstadoPedido estado) {
            when(pedidoRepository.buscarPorId(1L)).thenReturn(Optional.of(pedidoEn(estado)));

            assertThatThrownBy(() -> servicio.programar(1L, new EntregaRequest(1L, null, MANANA)))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("Solo se programa la entrega de un pedido CREADO o PAGADO");
            verify(entregaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Un pedido solo tiene una entrega")
        void yaTieneEntrega() {
            when(pedidoRepository.buscarPorId(1L)).thenReturn(Optional.of(pedidoEn(EstadoPedido.PAGADO)));
            when(entregaRepository.existsByIdPedido(1L)).thenReturn(true);

            assertThatThrownBy(() -> servicio.programar(1L, new EntregaRequest(1L, null, MANANA)))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("El pedido 1 ya tiene una entrega programada");
        }

        @Test
        @DisplayName("Un transportador inexistente lanza 404")
        void transportadorInexistente() {
            when(pedidoRepository.buscarPorId(1L)).thenReturn(Optional.of(pedidoEn(EstadoPedido.PAGADO)));
            when(entregaRepository.existsByIdPedido(1L)).thenReturn(false);
            when(transportadorRepository.buscarConUsuario(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.programar(1L, new EntregaRequest(99L, null, MANANA)))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessage("No existe el transportador con id 99");
        }
    }

    @Nested
    @DisplayName("Consultar por pedido")
    class ConsultarPorPedido {

        @Test
        @DisplayName("Devuelve la entrega del pedido")
        void consulta() {
            Entrega entrega = Entrega.programar(1L, transportador(), "Calle 1", MANANA);
            when(entregaRepository.buscarPorPedido(1L)).thenReturn(Optional.of(entrega));

            assertThat(servicio.consultarPorPedido(1L)).isSameAs(entrega);
        }

        @Test
        @DisplayName("Un pedido sin entrega lanza 404")
        void sinEntrega() {
            when(entregaRepository.buscarPorPedido(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.consultarPorPedido(1L))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessage("El pedido 1 no tiene una entrega programada");
        }
    }

    @Nested
    @DisplayName("Agenda del transportador")
    class ListarPorTransportador {

        @Test
        @DisplayName("Delega el filtro por estado al repositorio")
        void lista() {
            List<Entrega> agenda = List.of(Entrega.programar(1L, transportador(), "Calle 1", MANANA));
            when(transportadorRepository.existsById(1L)).thenReturn(true);
            when(entregaRepository.listarPorTransportador(1L, EstadoEntrega.PROGRAMADA)).thenReturn(agenda);

            assertThat(servicio.listarPorTransportador(1L, EstadoEntrega.PROGRAMADA)).isSameAs(agenda);
        }

        @Test
        @DisplayName("Un transportador inexistente lanza 404")
        void transportadorInexistente() {
            when(transportadorRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> servicio.listarPorTransportador(99L, null))
                    .isInstanceOf(RecursoNoEncontradoException.class);
            verify(entregaRepository, never()).listarPorTransportador(any(), any());
        }
    }

    private static Pedido pedidoEn(EstadoPedido estado) {
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setIdCliente(7L);
        pedido.setEstado(estado);
        return pedido;
    }

    private static Transportador transportador() {
        Transportador transportador = new Transportador();
        transportador.setId(1L);
        transportador.setPlaca("ABC123");
        return transportador;
    }

    private static Cliente clienteCon(String direccion) {
        Cliente cliente = new Cliente();
        cliente.setId(7L);
        cliente.setDireccion(direccion);
        return cliente;
    }
}
