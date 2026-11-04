package edu.itm.agrotech.controller;

import edu.itm.agrotech.dto.ResenaRequest;
import edu.itm.agrotech.dto.ResenaResponse;
import edu.itm.agrotech.dto.ResenasProductoResponse;
import edu.itm.agrotech.service.ResenaService;
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
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/** Calificaciones de los clientes a los productos que recibieron. */
@RestController
@RequestMapping("/api")
@Tag(name = "Resenas")
public class ResenaController {

    private final ResenaService resenaService;

    public ResenaController(ResenaService resenaService) {
        this.resenaService = resenaService;
    }

    @PostMapping("/productos/{idProducto}/resenas")
    @Operation(summary = "Resenar un producto",
            description = "Califica de 1 a 5 un producto. Solo si el cliente lo recibio en un pedido ENTREGADO, "
                    + "y una sola vez por producto.")
    @ApiResponse(responseCode = "201", description = "Resena registrada")
    @ApiResponse(responseCode = "400", description = "Datos invalidos o calificacion fuera de 1 a 5")
    @ApiResponse(responseCode = "404", description = "El producto o el cliente no existen")
    @ApiResponse(responseCode = "409", description = "El cliente no ha recibido el producto o ya lo reseno")
    public ResponseEntity<ResenaResponse> registrar(
            @Parameter(description = "Id del producto", example = "1") @PathVariable("idProducto") Long idProducto,
            @Valid @RequestBody ResenaRequest solicitud) {
        ResenaResponse respuesta = ResenaResponse.desde(resenaService.registrar(idProducto, solicitud));
        return ResponseEntity.created(URI.create("/api/productos/" + idProducto + "/resenas")).body(respuesta);
    }

    @GetMapping("/productos/{idProducto}/resenas")
    @Operation(summary = "Resenas de un producto",
            description = "De la mas reciente a la mas antigua, con la calificacion promedio y el total.")
    @ApiResponse(responseCode = "200", description = "Resenas del producto (puede no tener)")
    @ApiResponse(responseCode = "404", description = "El producto no existe")
    public ResponseEntity<ResenasProductoResponse> listarPorProducto(
            @Parameter(description = "Id del producto", example = "1") @PathVariable("idProducto") Long idProducto) {
        return ResponseEntity.ok(
                ResenasProductoResponse.desde(idProducto, resenaService.listarPorProducto(idProducto)));
    }

    @GetMapping("/clientes/{idCliente}/resenas")
    @Operation(summary = "Resenas escritas por un cliente",
            description = "De la mas reciente a la mas antigua.")
    @ApiResponse(responseCode = "200", description = "Resenas del cliente (puede no tener)")
    @ApiResponse(responseCode = "404", description = "El cliente no existe")
    public ResponseEntity<List<ResenaResponse>> listarPorCliente(
            @Parameter(description = "Id del cliente", example = "1") @PathVariable("idCliente") Long idCliente) {
        return ResponseEntity.ok(resenaService.listarPorCliente(idCliente)
                .stream()
                .map(ResenaResponse::desde)
                .toList());
    }
}
