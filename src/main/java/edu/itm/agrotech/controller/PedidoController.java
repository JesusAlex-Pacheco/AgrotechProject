package edu.itm.agrotech.controller;

import edu.itm.agrotech.domain.EstadoPedido;
import edu.itm.agrotech.domain.Pedido;
import edu.itm.agrotech.dto.CambioEstadoRequest;
import edu.itm.agrotech.dto.PedidoRequest;
import edu.itm.agrotech.dto.PedidoResponse;
import edu.itm.agrotech.security.ControlAcceso;
import edu.itm.agrotech.service.PedidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Capa de presentacion de los casos de negocio del pedido: la venta,
 * su consulta, los listados por cliente y por agricultor y el ciclo de vida.
 */
@RestController
@RequestMapping("/api")
@Tag(name = "Pedidos")
public class PedidoController {

    private final PedidoService pedidoService;
    private final ControlAcceso acceso;

    public PedidoController(PedidoService pedidoService, ControlAcceso acceso) {
        this.pedidoService = pedidoService;
        this.acceso = acceso;
    }

    @PostMapping("/pedidos")
    @Operation(summary = "Registrar una venta",
            description = "En una sola transaccion: valida cliente y existencias, congela el precio, "
                    + "calcula el total, descuenta el inventario y registra el pago como PENDIENTE. "
                    + "Requiere sesion de CLIENTE y solo compra a su propio nombre.")
    @ApiResponse(responseCode = "201", description = "Pedido registrado")
    @ApiResponse(responseCode = "400", description = "Datos invalidos")
    @ApiResponse(responseCode = "401", description = "No hay sesion")
    @ApiResponse(responseCode = "403", description = "No es cliente o compra a nombre de otro")
    @ApiResponse(responseCode = "404", description = "El cliente o algun producto no existen")
    @ApiResponse(responseCode = "409", description = "No hay existencias suficientes")
    public ResponseEntity<PedidoResponse> registrarVenta(@Valid @RequestBody PedidoRequest solicitud) {
        acceso.exigirCliente(solicitud.idCliente());
        PedidoResponse respuesta = PedidoResponse.desde(pedidoService.registrarVenta(solicitud));
        return ResponseEntity
                .created(URI.create("/api/pedidos/" + respuesta.id()))
                .body(respuesta);
    }

    @GetMapping("/pedidos/{id}")
    @Operation(summary = "Consultar un pedido",
            description = "Incluye sus lineas y su pago. Lo ven el cliente, los agricultores con productos "
                    + "en el pedido, el transportador asignado y el ADMIN.")
    @ApiResponse(responseCode = "200", description = "Pedido encontrado")
    @ApiResponse(responseCode = "401", description = "No hay sesion")
    @ApiResponse(responseCode = "403", description = "El pedido no es suyo")
    @ApiResponse(responseCode = "404", description = "El pedido no existe")
    public ResponseEntity<PedidoResponse> consultar(
            @Parameter(description = "Id del pedido", example = "1") @PathVariable("id") Long id) {
        acceso.exigirParticipanteDelPedido(id);
        return ResponseEntity.ok(PedidoResponse.desde(pedidoService.consultar(id)));
    }

    @GetMapping("/pedidos")
    @Operation(summary = "Historial de pedidos",
            description = "Del mas reciente al mas antiguo. Con idCliente, el historial de ese cliente (el propio "
                    + "cliente o el ADMIN); sin idCliente, todos los pedidos (solo ADMIN). Se puede filtrar por estado.")
    @ApiResponse(responseCode = "200", description = "Pedidos (puede estar vacio)")
    @ApiResponse(responseCode = "400", description = "El estado no es valido")
    @ApiResponse(responseCode = "401", description = "No hay sesion")
    @ApiResponse(responseCode = "403", description = "Consulta los pedidos de otro cliente o todos sin ser ADMIN")
    @ApiResponse(responseCode = "404", description = "El cliente no existe")
    public ResponseEntity<List<PedidoResponse>> listar(
            @Parameter(description = "Id del cliente", example = "1")
            @RequestParam(name = "idCliente", required = false) Long idCliente,
            @Parameter(description = "Filtro opcional por estado")
            @RequestParam(name = "estado", required = false) EstadoPedido estado) {

        List<Pedido> pedidos;
        if (idCliente == null) {
            acceso.exigirAdmin();
            pedidos = pedidoService.listarTodos(estado);
        } else {
            acceso.exigirClienteOAdmin(idCliente);
            pedidos = pedidoService.listarPorCliente(idCliente, estado);
        }
        return ResponseEntity.ok(pedidos.stream().map(PedidoResponse::desde).toList());
    }

    @GetMapping("/agricultores/{id}/pedidos")
    @Operation(summary = "Pedidos con productos de un agricultor",
            description = "Las ventas en las que aparece al menos un producto del agricultor, de la mas reciente "
                    + "a la mas antigua. Las consulta el propio agricultor o el ADMIN.")
    @ApiResponse(responseCode = "200", description = "Pedidos del agricultor (puede estar vacio)")
    @ApiResponse(responseCode = "400", description = "El estado no es valido")
    @ApiResponse(responseCode = "401", description = "No hay sesion")
    @ApiResponse(responseCode = "403", description = "Consulta los pedidos de otro agricultor")
    @ApiResponse(responseCode = "404", description = "El agricultor no existe")
    public ResponseEntity<List<PedidoResponse>> listarPorAgricultor(
            @Parameter(description = "Id del agricultor", example = "1") @PathVariable("id") Long id,
            @Parameter(description = "Filtro opcional por estado")
            @RequestParam(name = "estado", required = false) EstadoPedido estado) {
        acceso.exigirAgricultorOAdmin(id);
        return ResponseEntity.ok(pedidoService.listarPorAgricultor(id, estado)
                .stream()
                .map(PedidoResponse::desde)
                .toList());
    }

    @PatchMapping("/pedidos/{id}/estado")
    @Operation(summary = "Cambiar el estado de un pedido",
            description = "Ciclo de vida: CREADO -> PAGADO -> EN_RUTA -> ENTREGADO, y CANCELADO desde "
                    + "CREADO o PAGADO. Contra entrega pasa de CREADO a EN_RUTA y se cobra al entregar. "
                    + "Cancelar devuelve el inventario y rechaza el pago pendiente. Quien puede: PAGADO y EN_RUTA "
                    + "un agricultor con productos en el pedido; ENTREGADO el transportador asignado; CANCELADO "
                    + "el cliente o un agricultor del pedido. El ADMIN puede hacer cualquier cambio.")
    @ApiResponse(responseCode = "200", description = "Estado actualizado")
    @ApiResponse(responseCode = "400", description = "Estado ausente o desconocido")
    @ApiResponse(responseCode = "401", description = "No hay sesion")
    @ApiResponse(responseCode = "403", description = "Su rol no puede hacer ese cambio en este pedido")
    @ApiResponse(responseCode = "404", description = "El pedido no existe")
    @ApiResponse(responseCode = "409", description = "La transicion no esta permitida")
    public ResponseEntity<PedidoResponse> cambiarEstado(
            @Parameter(description = "Id del pedido", example = "1") @PathVariable("id") Long id,
            @Valid @RequestBody CambioEstadoRequest solicitud) {
        acceso.exigirPermisoParaCambiarEstado(id, solicitud.estado());
        return ResponseEntity.ok(PedidoResponse.desde(pedidoService.cambiarEstado(id, solicitud.estado())));
    }
}
