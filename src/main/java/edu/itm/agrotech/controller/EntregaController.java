package edu.itm.agrotech.controller;

import edu.itm.agrotech.domain.EstadoEntrega;
import edu.itm.agrotech.dto.EntregaRequest;
import edu.itm.agrotech.dto.EntregaResponse;
import edu.itm.agrotech.service.EntregaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/** Logistica: programar la entrega de un pedido y la agenda de cada transportador. */
@RestController
@RequestMapping("/api")
@Tag(name = "Entregas")
public class EntregaController {

    private final EntregaService entregaService;

    public EntregaController(EntregaService entregaService) {
        this.entregaService = entregaService;
    }

    @PostMapping("/pedidos/{idPedido}/entrega")
    @Operation(summary = "Programar la entrega de un pedido",
            description = "Asigna transportador, direccion y fecha a un pedido CREADO o PAGADO. Sin direccion, "
                    + "se usa la del cliente. Es requisito para despachar el pedido (pasarlo a EN_RUTA); desde "
                    + "ahi la entrega avanza sola con el pedido.")
    @ApiResponse(responseCode = "201", description = "Entrega programada")
    @ApiResponse(responseCode = "400", description = "Datos invalidos o fecha en el pasado")
    @ApiResponse(responseCode = "404", description = "El pedido o el transportador no existen")
    @ApiResponse(responseCode = "409", description = "El pedido ya salio, ya tiene entrega o no hay direccion")
    public ResponseEntity<EntregaResponse> programar(
            @Parameter(description = "Id del pedido", example = "1") @PathVariable("idPedido") Long idPedido,
            @Valid @RequestBody EntregaRequest solicitud) {
        EntregaResponse respuesta = EntregaResponse.desde(entregaService.programar(idPedido, solicitud));
        return ResponseEntity.created(URI.create("/api/pedidos/" + idPedido + "/entrega")).body(respuesta);
    }

    @GetMapping("/pedidos/{idPedido}/entrega")
    @Operation(summary = "Consultar la entrega de un pedido")
    @ApiResponse(responseCode = "200", description = "Entrega encontrada")
    @ApiResponse(responseCode = "404", description = "El pedido no tiene entrega programada")
    public ResponseEntity<EntregaResponse> consultarPorPedido(
            @Parameter(description = "Id del pedido", example = "1") @PathVariable("idPedido") Long idPedido) {
        return ResponseEntity.ok(EntregaResponse.desde(entregaService.consultarPorPedido(idPedido)));
    }

    @GetMapping("/transportadores/{id}/entregas")
    @Operation(summary = "Agenda de un transportador",
            description = "Sus entregas ordenadas por fecha programada. Se puede filtrar por estado.")
    @ApiResponse(responseCode = "200", description = "Entregas del transportador (puede estar vacia)")
    @ApiResponse(responseCode = "400", description = "El estado no es valido")
    @ApiResponse(responseCode = "404", description = "El transportador no existe")
    public ResponseEntity<List<EntregaResponse>> listarPorTransportador(
            @Parameter(description = "Id del transportador", example = "1") @PathVariable("id") Long id,
            @Parameter(description = "Filtro opcional por estado")
            @RequestParam(name = "estado", required = false) EstadoEntrega estado) {

        List<EntregaResponse> entregas = entregaService.listarPorTransportador(id, estado)
                .stream()
                .map(EntregaResponse::desde)
                .toList();

        return ResponseEntity.ok(entregas);
    }
}
