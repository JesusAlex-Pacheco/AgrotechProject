package edu.itm.agrotech.security;

import edu.itm.agrotech.domain.EstadoPedido;
import edu.itm.agrotech.domain.Producto;
import edu.itm.agrotech.exception.AccesoDenegadoException;
import edu.itm.agrotech.repository.EntregaRepository;
import edu.itm.agrotech.repository.PedidoRepository;
import edu.itm.agrotech.repository.ProductoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ControlAccesoTest {

    private static final Long PEDIDO = 10L;

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private EntregaRepository entregaRepository;

    private ControlAcceso acceso;

    @BeforeEach
    void preparar() {
        acceso = new ControlAcceso(pedidoRepository, productoRepository, entregaRepository);
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    /** Simula la sesion que crea Spring Security al validar el token. */
    private static void sesion(String rol, Long idPerfil) {
        Jwt.Builder jwt = Jwt.withTokenValue("token").header("alg", "HS256").subject("1").claim("rol", rol);
        if (idPerfil != null) {
            jwt.claim("idPerfil", idPerfil);
        }
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt.build()));
    }

    /** Pedido 10: del cliente 3, con productos del agricultor 2, llevado por el transportador 4. */
    private void pedidoDeEjemplo() {
        lenient().when(pedidoRepository.buscarIdCliente(PEDIDO)).thenReturn(Optional.of(3L));
        lenient().when(pedidoRepository.agricultoresDelPedido(PEDIDO)).thenReturn(List.of(2L));
        lenient().when(entregaRepository.buscarIdTransportadorPorPedido(PEDIDO)).thenReturn(Optional.of(4L));
    }

    @Test
    @DisplayName("Sin sesion no se puede identificar al usuario")
    void sinSesion() {
        assertThatThrownBy(() -> acceso.exigirCliente(3L))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Nested
    @DisplayName("Cada perfil actua solo a su nombre")
    class Perfiles {

        @Test
        @DisplayName("Un cliente compra a su nombre, pero no al de otro")
        void cliente() {
            sesion("CLIENTE", 3L);

            assertThatCode(() -> acceso.exigirCliente(3L)).doesNotThrowAnyException();
            assertThatThrownBy(() -> acceso.exigirCliente(8L)).isInstanceOf(AccesoDenegadoException.class);
        }

        @Test
        @DisplayName("Un agricultor con el mismo id de perfil no pasa por cliente")
        void rolDistinto() {
            sesion("AGRICULTOR", 3L);

            assertThatThrownBy(() -> acceso.exigirCliente(3L)).isInstanceOf(AccesoDenegadoException.class);
        }

        @Test
        @DisplayName("El ADMIN consulta cualquier cuenta, pero no compra por otros")
        void admin() {
            sesion("ADMIN", null);

            assertThatCode(() -> acceso.exigirClienteOAdmin(3L)).doesNotThrowAnyException();
            assertThatCode(() -> acceso.exigirAdmin()).doesNotThrowAnyException();
            assertThatThrownBy(() -> acceso.exigirCliente(3L)).isInstanceOf(AccesoDenegadoException.class);
        }

        @Test
        @DisplayName("Solo el ADMIN ve todos los pedidos")
        void soloAdmin() {
            sesion("CLIENTE", 3L);

            assertThatThrownBy(() -> acceso.exigirAdmin()).isInstanceOf(AccesoDenegadoException.class);
        }
    }

    @Nested
    @DisplayName("Productos")
    class Productos {

        @Test
        @DisplayName("Solo el agricultor que lo publico modifica el producto")
        void dueno() {
            Producto producto = new Producto();
            producto.setIdAgricultor(2L);
            when(productoRepository.buscarPorId(5L)).thenReturn(Optional.of(producto));

            sesion("AGRICULTOR", 2L);
            assertThatCode(() -> acceso.exigirDuenoDelProducto(5L)).doesNotThrowAnyException();

            sesion("AGRICULTOR", 9L);
            assertThatThrownBy(() -> acceso.exigirDuenoDelProducto(5L))
                    .isInstanceOf(AccesoDenegadoException.class);
        }
    }

    @Nested
    @DisplayName("Pedidos")
    class Pedidos {

        @Test
        @DisplayName("Ven el pedido el cliente, el agricultor y el transportador, pero no otros")
        void participantes() {
            pedidoDeEjemplo();

            sesion("CLIENTE", 3L);
            assertThatCode(() -> acceso.exigirParticipanteDelPedido(PEDIDO)).doesNotThrowAnyException();
            sesion("AGRICULTOR", 2L);
            assertThatCode(() -> acceso.exigirParticipanteDelPedido(PEDIDO)).doesNotThrowAnyException();
            sesion("TRANSPORTADOR", 4L);
            assertThatCode(() -> acceso.exigirParticipanteDelPedido(PEDIDO)).doesNotThrowAnyException();

            sesion("CLIENTE", 8L);
            assertThatThrownBy(() -> acceso.exigirParticipanteDelPedido(PEDIDO))
                    .isInstanceOf(AccesoDenegadoException.class);
        }

        @Test
        @DisplayName("Un pedido que no existe pasa, para que el servicio responda 404")
        void inexistente() {
            when(pedidoRepository.buscarIdCliente(99L)).thenReturn(Optional.empty());
            sesion("CLIENTE", 8L);

            assertThatCode(() -> acceso.exigirParticipanteDelPedido(99L)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("El agricultor del pedido confirma el pago y despacha; el cliente no")
        void pagarYDespachar() {
            pedidoDeEjemplo();

            sesion("AGRICULTOR", 2L);
            assertThatCode(() -> acceso.exigirPermisoParaCambiarEstado(PEDIDO, EstadoPedido.PAGADO))
                    .doesNotThrowAnyException();
            assertThatCode(() -> acceso.exigirPermisoParaCambiarEstado(PEDIDO, EstadoPedido.EN_RUTA))
                    .doesNotThrowAnyException();

            sesion("CLIENTE", 3L);
            assertThatThrownBy(() -> acceso.exigirPermisoParaCambiarEstado(PEDIDO, EstadoPedido.PAGADO))
                    .isInstanceOf(AccesoDenegadoException.class);
        }

        @Test
        @DisplayName("Solo el transportador asignado marca el pedido como entregado")
        void entregar() {
            pedidoDeEjemplo();

            sesion("TRANSPORTADOR", 4L);
            assertThatCode(() -> acceso.exigirPermisoParaCambiarEstado(PEDIDO, EstadoPedido.ENTREGADO))
                    .doesNotThrowAnyException();

            sesion("TRANSPORTADOR", 6L);
            assertThatThrownBy(() -> acceso.exigirPermisoParaCambiarEstado(PEDIDO, EstadoPedido.ENTREGADO))
                    .isInstanceOf(AccesoDenegadoException.class);
            sesion("AGRICULTOR", 2L);
            assertThatThrownBy(() -> acceso.exigirPermisoParaCambiarEstado(PEDIDO, EstadoPedido.ENTREGADO))
                    .isInstanceOf(AccesoDenegadoException.class);
        }

        @Test
        @DisplayName("Cancelan el cliente del pedido y su agricultor; otro agricultor no")
        void cancelar() {
            pedidoDeEjemplo();

            sesion("CLIENTE", 3L);
            assertThatCode(() -> acceso.exigirPermisoParaCambiarEstado(PEDIDO, EstadoPedido.CANCELADO))
                    .doesNotThrowAnyException();
            sesion("AGRICULTOR", 2L);
            assertThatCode(() -> acceso.exigirPermisoParaCambiarEstado(PEDIDO, EstadoPedido.CANCELADO))
                    .doesNotThrowAnyException();

            sesion("AGRICULTOR", 9L);
            assertThatThrownBy(() -> acceso.exigirPermisoParaCambiarEstado(PEDIDO, EstadoPedido.CANCELADO))
                    .isInstanceOf(AccesoDenegadoException.class);
        }

        @Test
        @DisplayName("El ADMIN puede hacer cualquier cambio de estado")
        void admin() {
            sesion("ADMIN", null);

            assertThatCode(() -> acceso.exigirPermisoParaCambiarEstado(PEDIDO, EstadoPedido.ENTREGADO))
                    .doesNotThrowAnyException();
        }
    }
}
