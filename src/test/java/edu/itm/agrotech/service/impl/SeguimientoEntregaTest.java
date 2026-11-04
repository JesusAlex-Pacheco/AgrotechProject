package edu.itm.agrotech.service.impl;

import edu.itm.agrotech.domain.EstadoEntrega;
import edu.itm.agrotech.domain.EstadoPedido;
import edu.itm.agrotech.domain.Pedido;
import edu.itm.agrotech.entity.Entrega;
import edu.itm.agrotech.exception.ReglaNegocioException;
import edu.itm.agrotech.repository.EntregaRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeguimientoEntregaTest {

    @Mock
    private EntregaRepository entregaRepository;

    private SeguimientoEntrega seguimiento;

    @BeforeEach
    void preparar() {
        seguimiento = new SeguimientoEntrega(entregaRepository);
    }

    @Nested
    @DisplayName("Validar el cambio")
    class ValidarCambio {

        @Test
        @DisplayName("No deja despachar un pedido sin entrega programada")
        void despacharSinEntrega() {
            when(entregaRepository.existsByIdPedido(1L)).thenReturn(false);

            assertThatThrownBy(() -> seguimiento.validarCambio(pedido(), EstadoPedido.EN_RUTA))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("Programe la entrega del pedido 1");
        }

        @Test
        @DisplayName("Deja despachar si la entrega esta programada")
        void despacharConEntrega() {
            when(entregaRepository.existsByIdPedido(1L)).thenReturn(true);

            assertThatCode(() -> seguimiento.validarCambio(pedido(), EstadoPedido.EN_RUTA))
                    .doesNotThrowAnyException();
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = EstadoPedido.class, names = {"PAGADO", "ENTREGADO", "CANCELADO"})
        @DisplayName("Los demas cambios no dependen de la entrega")
        void otrosEstados(EstadoPedido estado) {
            seguimiento.validarCambio(pedido(), estado);

            verify(entregaRepository, never()).existsByIdPedido(anyLong());
        }
    }

    @Nested
    @DisplayName("Registrar el cambio")
    class RegistrarCambio {

        @Test
        @DisplayName("EN_RUTA: la entrega sale a ruta")
        void enRuta() {
            Entrega entrega = entregaProgramada();
            when(entregaRepository.findByIdPedido(1L)).thenReturn(Optional.of(entrega));

            seguimiento.registrarCambio(pedido(), EstadoPedido.EN_RUTA);

            assertThat(entrega.getEstado()).isEqualTo(EstadoEntrega.EN_RUTA);
            verify(entregaRepository).save(entrega);
        }

        @Test
        @DisplayName("ENTREGADO: la entrega queda ENTREGADA con la fecha de hoy")
        void entregado() {
            Entrega entrega = entregaProgramada();
            when(entregaRepository.findByIdPedido(1L)).thenReturn(Optional.of(entrega));

            seguimiento.registrarCambio(pedido(), EstadoPedido.ENTREGADO);

            assertThat(entrega.getEstado()).isEqualTo(EstadoEntrega.ENTREGADA);
            assertThat(entrega.getFechaEntrega()).isEqualTo(LocalDate.now());
            verify(entregaRepository).save(entrega);
        }

        @Test
        @DisplayName("CANCELADO: la entrega queda FALLIDA")
        void cancelado() {
            Entrega entrega = entregaProgramada();
            when(entregaRepository.findByIdPedido(1L)).thenReturn(Optional.of(entrega));

            seguimiento.registrarCambio(pedido(), EstadoPedido.CANCELADO);

            assertThat(entrega.getEstado()).isEqualTo(EstadoEntrega.FALLIDA);
            verify(entregaRepository).save(entrega);
        }

        @Test
        @DisplayName("PAGADO: la entrega no cambia")
        void pagado() {
            Entrega entrega = entregaProgramada();
            when(entregaRepository.findByIdPedido(1L)).thenReturn(Optional.of(entrega));

            seguimiento.registrarCambio(pedido(), EstadoPedido.PAGADO);

            assertThat(entrega.getEstado()).isEqualTo(EstadoEntrega.PROGRAMADA);
            verify(entregaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Un pedido sin entrega no requiere nada")
        void sinEntrega() {
            when(entregaRepository.findByIdPedido(1L)).thenReturn(Optional.empty());

            seguimiento.registrarCambio(pedido(), EstadoPedido.CANCELADO);

            verify(entregaRepository, never()).save(any());
        }
    }

    private static Pedido pedido() {
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        return pedido;
    }

    private static Entrega entregaProgramada() {
        return Entrega.programar(1L, null, "Calle 1", LocalDate.now().plusDays(1));
    }
}
