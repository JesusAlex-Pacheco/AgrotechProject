package edu.itm.agrotech.security;

import edu.itm.agrotech.domain.EstadoPedido;
import edu.itm.agrotech.domain.Producto;
import edu.itm.agrotech.domain.Rol;
import edu.itm.agrotech.exception.AccesoDenegadoException;
import edu.itm.agrotech.repository.EntregaRepository;
import edu.itm.agrotech.repository.PedidoRepository;
import edu.itm.agrotech.repository.ProductoRepository;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Segunda capa de autorizacion. SeguridadConfig ya exige el rol por ruta
 * (por ejemplo, solo un CLIENTE llega a POST /api/pedidos); aqui se verifica
 * que el recurso sea del usuario: que el cliente compre a su nombre, que el
 * agricultor edite sus propios productos, que el transportador entregue los
 * pedidos que tiene asignados. El ADMIN puede hacerlo todo.
 *
 * <p>Si un pedido o producto no existe, se deja pasar: el servicio responde
 * 404 como lo hace sin seguridad.
 */
@Component
public class ControlAcceso {

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final EntregaRepository entregaRepository;

    public ControlAcceso(PedidoRepository pedidoRepository,
                         ProductoRepository productoRepository,
                         EntregaRepository entregaRepository) {
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
        this.entregaRepository = entregaRepository;
    }

    /** Usuario de la solicitud en curso, leido de su token. */
    public UsuarioActual usuarioActual() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (!(autenticacion instanceof JwtAuthenticationToken token)) {
            throw new AuthenticationCredentialsNotFoundException("No hay una sesion iniciada");
        }
        Jwt jwt = token.getToken();
        Number idPerfil = jwt.getClaim(ServicioTokens.CLAIM_ID_PERFIL);
        return new UsuarioActual(
                Long.valueOf(jwt.getSubject()),
                Rol.valueOf(jwt.getClaimAsString(ServicioTokens.CLAIM_ROL)),
                idPerfil == null ? null : idPerfil.longValue());
    }

    // ---------------------------------------------------------------
    // Perfiles: cada usuario actua solo en nombre de si mismo
    // ---------------------------------------------------------------

    public void exigirAdmin() {
        if (!usuarioActual().esAdmin()) {
            throw new AccesoDenegadoException("Solo el administrador puede ver todos los pedidos");
        }
    }

    /** Comprar o resenar: solo el propio cliente, ni siquiera el ADMIN lo hace por el. */
    public void exigirCliente(Long idCliente) {
        if (!usuarioActual().es(Rol.CLIENTE, idCliente)) {
            throw new AccesoDenegadoException("Solo puede comprar o resenar a su propio nombre");
        }
    }

    public void exigirClienteOAdmin(Long idCliente) {
        UsuarioActual usuario = usuarioActual();
        if (!usuario.esAdmin() && !usuario.es(Rol.CLIENTE, idCliente)) {
            throw new AccesoDenegadoException("Solo puede consultar la informacion de su propia cuenta");
        }
    }

    public void exigirAgricultorOAdmin(Long idAgricultor) {
        UsuarioActual usuario = usuarioActual();
        if (!usuario.esAdmin() && !usuario.es(Rol.AGRICULTOR, idAgricultor)) {
            throw new AccesoDenegadoException("Solo puede actuar a nombre de su propia finca");
        }
    }

    public void exigirTransportadorOAdmin(Long idTransportador) {
        UsuarioActual usuario = usuarioActual();
        if (!usuario.esAdmin() && !usuario.es(Rol.TRANSPORTADOR, idTransportador)) {
            throw new AccesoDenegadoException("Solo puede consultar su propia agenda de entregas");
        }
    }

    // ---------------------------------------------------------------
    // Productos
    // ---------------------------------------------------------------

    /** Editar o retirar un producto: el agricultor que lo publico o el ADMIN. */
    public void exigirDuenoDelProducto(Long idProducto) {
        UsuarioActual usuario = usuarioActual();
        if (usuario.esAdmin()) {
            return;
        }
        Optional<Long> idAgricultor = productoRepository.buscarPorId(idProducto).map(Producto::getIdAgricultor);
        if (idAgricultor.isPresent() && !usuario.es(Rol.AGRICULTOR, idAgricultor.get())) {
            throw new AccesoDenegadoException("Solo el agricultor que publico el producto puede modificarlo");
        }
    }

    // ---------------------------------------------------------------
    // Pedidos y entregas
    // ---------------------------------------------------------------

    /** Ver un pedido o su entrega: el cliente, un agricultor que vende en el, su transportador o el ADMIN. */
    public void exigirParticipanteDelPedido(Long idPedido) {
        UsuarioActual usuario = usuarioActual();
        if (usuario.esAdmin() || !existePedido(idPedido)) {
            return;
        }
        if (!esClienteDelPedido(usuario, idPedido)
                && !esVendedorDelPedido(usuario, idPedido)
                && !esTransportadorDelPedido(usuario, idPedido)) {
            throw new AccesoDenegadoException("El pedido " + idPedido + " no es suyo");
        }
    }

    /** Programar la entrega: un agricultor que vende en el pedido o el ADMIN. */
    public void exigirVendedorDelPedido(Long idPedido) {
        UsuarioActual usuario = usuarioActual();
        if (usuario.esAdmin() || !existePedido(idPedido)) {
            return;
        }
        if (!esVendedorDelPedido(usuario, idPedido)) {
            throw new AccesoDenegadoException(
                    "Solo un agricultor con productos en el pedido puede programar su entrega");
        }
    }

    /**
     * Quien mueve el pedido por su ciclo de vida:
     * <ul>
     *   <li>PAGADO y EN_RUTA: un agricultor que vende en el pedido (confirma el pago y despacha).</li>
     *   <li>ENTREGADO: el transportador asignado a la entrega.</li>
     *   <li>CANCELADO: el cliente que compro o un agricultor que vende en el pedido.</li>
     * </ul>
     * El ADMIN puede hacer cualquier cambio. Si la transicion es valida o no
     * lo decide despues PedidoService.
     */
    public void exigirPermisoParaCambiarEstado(Long idPedido, EstadoPedido nuevoEstado) {
        UsuarioActual usuario = usuarioActual();
        if (usuario.esAdmin() || !existePedido(idPedido)) {
            return;
        }
        boolean permitido = switch (nuevoEstado) {
            case PAGADO, EN_RUTA -> esVendedorDelPedido(usuario, idPedido);
            case ENTREGADO -> esTransportadorDelPedido(usuario, idPedido);
            case CANCELADO -> esClienteDelPedido(usuario, idPedido) || esVendedorDelPedido(usuario, idPedido);
            case CREADO -> false;
        };
        if (!permitido) {
            throw new AccesoDenegadoException(switch (nuevoEstado) {
                case PAGADO, EN_RUTA -> "Solo un agricultor con productos en el pedido puede confirmar el pago o despacharlo";
                case ENTREGADO -> "Solo el transportador asignado puede marcar el pedido como entregado";
                case CANCELADO -> "Solo el cliente o un agricultor con productos en el pedido puede cancelarlo";
                case CREADO -> "Un pedido no puede volver al estado CREADO";
            });
        }
    }

    private boolean existePedido(Long idPedido) {
        return pedidoRepository.buscarIdCliente(idPedido).isPresent();
    }

    private boolean esClienteDelPedido(UsuarioActual usuario, Long idPedido) {
        return usuario.rol() == Rol.CLIENTE
                && pedidoRepository.buscarIdCliente(idPedido).filter(id -> usuario.es(Rol.CLIENTE, id)).isPresent();
    }

    private boolean esVendedorDelPedido(UsuarioActual usuario, Long idPedido) {
        return usuario.rol() == Rol.AGRICULTOR
                && pedidoRepository.agricultoresDelPedido(idPedido).stream()
                        .anyMatch(id -> usuario.es(Rol.AGRICULTOR, id));
    }

    private boolean esTransportadorDelPedido(UsuarioActual usuario, Long idPedido) {
        return usuario.rol() == Rol.TRANSPORTADOR
                && entregaRepository.buscarIdTransportadorPorPedido(idPedido)
                        .filter(id -> usuario.es(Rol.TRANSPORTADOR, id)).isPresent();
    }
}
