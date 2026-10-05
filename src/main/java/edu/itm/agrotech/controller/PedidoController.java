package edu.itm.agrotech.controller;

import edu.itm.agrotech.domain.EstadoPedido;
import edu.itm.agrotech.dto.CambioEstadoRequest;
import edu.itm.agrotech.dto.PedidoRequest;
import edu.itm.agrotech.dto.PedidoResponse;
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
 * su consulta, el historial del cliente y el ciclo de vida.
 */
@RestController
@RequestMapping("/api/pedidos")
@Tag(name = "Pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    @Operation(summary = "Registrar una venta",
            description = "En una sola transaccion: valida cliente y existencias, congela el precio, "
                    + "calcula el total, descuenta el inventario y registra el pago como PENDIENTE.")
    @ApiResponse(responseCode = "201", description = "Pedido registrado")
    @ApiResponse(responseCode = "400", description = "Datos invalidos")
    @ApiResponse(responseCode = "404", description = "El cliente o algun producto no existen")
    @ApiResponse(responseCode = "409", description = "No hay existencias suficientes")
    public ResponseEntity<PedidoResponse> registrarVenta(@Valid @RequestBody PedidoRequest solicitud) {
        PedidoResponse respuesta = PedidoResponse.desde(pedidoService.registrarVenta(solicitud));
        return ResponseEntity
                .created(URI.create("/api/pedidos/" + respuesta.id()))
                .body(respuesta);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un pedido", description = "Incluye sus lineas y su pago.")
    @ApiResponse(responseCode = "200", description = "Pedido encontrado")
    @ApiResponse(responseCode = "404", description = "El pedido no existe")
    public ResponseEntity<PedidoResponse> consultar(
            @Parameter(description = "Id del pedido", example = "1") @PathVariable("id") Long id) {
        return ResponseEntity.ok(PedidoResponse.desde(pedidoService.consultar(id)));
    }

    @GetMapping
    @Operation(summary = "Historial de pedidos de un cliente",
            description = "Del mas reciente al mas antiguo. Se puede filtrar por estado.")
    @ApiResponse(responseCode = "200", description = "Historial del cliente (puede estar vacio)")
    @ApiResponse(responseCode = "400", description = "Falta idCliente o el estado no es valido")
    @ApiResponse(responseCode = "404", description = "El cliente no existe")
    public ResponseEntity<List<PedidoResponse>> listarPorCliente(
            @Parameter(description = "Id del cliente", example = "1")
            @RequestParam(name = "idCliente") Long idCliente,
            @Parameter(description = "Filtro opcional por estado")
            @RequestParam(name = "estado", required = false) EstadoPedido estado) {

        List<PedidoResponse> pedidos = pedidoService.listarPorCliente(idCliente, estado)
                .stream()
                .map(PedidoResponse::desde)
                .toList();

        return ResponseEntity.ok(pedidos);
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Cambiar el estado de un pedido",
            description = "Ciclo de vida: CREADO -> PAGADO -> EN_RUTA -> ENTREGADO, y CANCELADO desde "
                    + "CREADO o PAGADO. Contra entrega pasa de CREADO a EN_RUTA y se cobra al entregar. "
                    + "Cancelar devuelve el inventario y rechaza el pago pendiente.")
    @ApiResponse(responseCode = "200", description = "Estado actualizado")
    @ApiResponse(responseCode = "400", description = "Estado ausente o desconocido")
    @ApiResponse(responseCode = "404", description = "El pedido no existe")
    @ApiResponse(responseCode = "409", description = "La transicion no esta permitida")
    public ResponseEntity<PedidoResponse> cambiarEstado(
            @Parameter(description = "Id del pedido", example = "1") @PathVariable("id") Long id,
            @Valid @RequestBody CambioEstadoRequest solicitud) {
        return ResponseEntity.ok(PedidoResponse.desde(pedidoService.cambiarEstado(id, solicitud.estado())));
    }
}
