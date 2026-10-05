package edu.itm.agrotech.service.impl;

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
import edu.itm.agrotech.repository.ClienteRepository;
import edu.itm.agrotech.repository.PedidoRepository;
import edu.itm.agrotech.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoServiceImplTest {

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private ClienteRepository clienteRepository;

    private PedidoServiceImpl servicio;

    @BeforeEach
    void preparar() {
        servicio = new PedidoServiceImpl(pedidoRepository, productoRepository, clienteRepository);
    }

    @Nested
    @DisplayName("Registrar una venta")
    class RegistrarVenta {

        @Test
        @DisplayName("Calcula el total con el precio del momento y deja el pago PENDIENTE")
        void calculaTotal() {
            when(clienteRepository.existe(1L)).thenReturn(true);
            when(productoRepository.buscarPorId(1L)).thenReturn(Optional.of(producto(1L, "3000", "120")));
            when(productoRepository.buscarPorId(2L)).thenReturn(Optional.of(producto(2L, "1500", "60")));
            when(pedidoRepository.registrarVenta(any())).thenAnswer(inv -> inv.getArgument(0));

            Pedido pedido = servicio.registrarVenta(new PedidoRequest(1L, MetodoPago.PSE,
                    new BigDecimal("6000"), List.of(item(1L, "3"), item(2L, "2"))));

            // 3 x 3000 + 2 x 1500 = 12000; + 6000 de envio = 18000
            assertThat(pedido.getSubtotal()).isEqualByComparingTo("12000");
            assertThat(pedido.getTotal()).isEqualByComparingTo("18000");
            assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.CREADO);
            assertThat(pedido.getPago().getEstado()).isEqualTo(EstadoPago.PENDIENTE);
            assertThat(pedido.getPago().getMonto()).isEqualByComparingTo("18000");
        }

        @Test
        @DisplayName("Rechaza la venta si no hay existencias suficientes")
        void sinExistencias() {
            when(clienteRepository.existe(1L)).thenReturn(true);
            when(productoRepository.buscarPorId(1L)).thenReturn(Optional.of(producto(1L, "3000", "120")));

            assertThatThrownBy(() -> servicio.registrarVenta(new PedidoRequest(1L, MetodoPago.PSE,
                    null, List.of(item(1L, "99999")))))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("no tiene existencias suficientes");
            verify(pedidoRepository, never()).registrarVenta(any());
        }

        @Test
        @DisplayName("Rechaza un cliente que no existe")
        void clienteInexistente() {
            when(clienteRepository.existe(99L)).thenReturn(false);

            assertThatThrownBy(() -> servicio.registrarVenta(new PedidoRequest(99L, MetodoPago.PSE,
                    null, List.of(item(1L, "1")))))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessage("No existe el cliente con id 99");
        }
    }

    @Nested
    @DisplayName("Cambiar el estado")
    class CambiarEstado {

        @Test
        @DisplayName("PSE: al pagar, el pago queda APROBADO con fecha")
        void pagarConPse() {
            Pedido pedido = pedidoEn(EstadoPedido.CREADO, MetodoPago.PSE);
            when(pedidoRepository.buscarPorId(1L)).thenReturn(Optional.of(pedido));

            servicio.cambiarEstado(1L, EstadoPedido.PAGADO);

            assertThat(pedido.getEstado()).isEqualTo(EstadoPedido.PAGADO);
            assertThat(pedido.getPago().getEstado()).isEqualTo(EstadoPago.APROBADO);
            assertThat(pedido.getPago().getFechaPago()).isNotNull();
            verify(pedidoRepository).actualizarEstado(pedido, EstadoPedido.CREADO, false);
        }

        @Test
        @DisplayName("PSE: no se puede despachar sin pagar")
        void despacharSinPagar() {
            when(pedidoRepository.buscarPorId(1L))
                    .thenReturn(Optional.of(pedidoEn(EstadoPedido.CREADO, MetodoPago.TARJETA)));

            assertThatThrownBy(() -> servicio.cambiarEstado(1L, EstadoPedido.EN_RUTA))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("debe estar PAGADO antes de despacharse");
            verify(pedidoRepository, never()).actualizarEstado(any(), any(), anyBoolean());
        }

        @Test
        @DisplayName("Contra entrega: no se marca PAGADO antes de entregar")
        void contraEntregaNoSePagaAntes() {
            when(pedidoRepository.buscarPorId(1L))
                    .thenReturn(Optional.of(pedidoEn(EstadoPedido.CREADO, MetodoPago.CONTRA_ENTREGA)));

            assertThatThrownBy(() -> servicio.cambiarEstado(1L, EstadoPedido.PAGADO))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("se paga al entregarse");
        }

        @Test
        @DisplayName("Contra entrega: al entregar se cobra el pago")
        void contraEntregaSeCobraAlEntregar() {
            Pedido pedido = pedidoEn(EstadoPedido.EN_RUTA, MetodoPago.CONTRA_ENTREGA);
            when(pedidoRepository.buscarPorId(1L)).thenReturn(Optional.of(pedido));

            servicio.cambiarEstado(1L, EstadoPedido.ENTREGADO);

            assertThat(pedido.getPago().getEstado()).isEqualTo(EstadoPago.APROBADO);
        }

        @Test
        @DisplayName("Cancelar: devuelve el inventario y rechaza el pago pendiente")
        void cancelar() {
            Pedido pedido = pedidoEn(EstadoPedido.CREADO, MetodoPago.PSE);
            when(pedidoRepository.buscarPorId(1L)).thenReturn(Optional.of(pedido));

            servicio.cambiarEstado(1L, EstadoPedido.CANCELADO);

            assertThat(pedido.getPago().getEstado()).isEqualTo(EstadoPago.RECHAZADO);
            verify(pedidoRepository).actualizarEstado(pedido, EstadoPedido.CREADO, true);
        }

        @Test
        @DisplayName("Un pedido entregado ya no se puede cancelar")
        void entregadoEsFinal() {
            when(pedidoRepository.buscarPorId(1L))
                    .thenReturn(Optional.of(pedidoEn(EstadoPedido.ENTREGADO, MetodoPago.PSE)));

            assertThatThrownBy(() -> servicio.cambiarEstado(1L, EstadoPedido.CANCELADO))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("No se puede pasar un pedido de ENTREGADO a CANCELADO");
        }
    }

    @Nested
    @DisplayName("Historial del cliente")
    class Historial {

        @Test
        @DisplayName("Rechaza un cliente que no existe")
        void clienteInexistente() {
            when(clienteRepository.existe(99L)).thenReturn(false);

            assertThatThrownBy(() -> servicio.listarPorCliente(99L, null))
                    .isInstanceOf(RecursoNoEncontradoException.class);
            verify(pedidoRepository, never()).listarPorCliente(any(), any());
        }

        @Test
        @DisplayName("Delega el filtro por estado al repositorio")
        void filtraPorEstado() {
            when(clienteRepository.existe(1L)).thenReturn(true);
            when(pedidoRepository.listarPorCliente(1L, EstadoPedido.ENTREGADO)).thenReturn(List.of());

            assertThat(servicio.listarPorCliente(1L, EstadoPedido.ENTREGADO)).isEmpty();
            verify(pedidoRepository).listarPorCliente(eq(1L), eq(EstadoPedido.ENTREGADO));
        }
    }

    private static Producto producto(Long id, String precio, String stock) {
        Producto producto = new Producto();
        producto.setId(id);
        producto.setNombre("Producto " + id);
        producto.setPrecio(new BigDecimal(precio));
        producto.setStock(new BigDecimal(stock));
        producto.setDisponible(true);
        return producto;
    }

    private static ItemPedidoRequest item(Long idProducto, String cantidad) {
        return new ItemPedidoRequest(idProducto, new BigDecimal(cantidad));
    }

    private static Pedido pedidoEn(EstadoPedido estado, MetodoPago metodo) {
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setEstado(estado);
        pedido.setPago(Pago.pendiente(metodo, new BigDecimal("18000")));
        return pedido;
    }
}
