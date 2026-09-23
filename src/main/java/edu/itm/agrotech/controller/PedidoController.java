package edu.itm.agrotech.controller;

import edu.itm.agrotech.dto.PedidoRequest;
import edu.itm.agrotech.dto.PedidoResponse;
import edu.itm.agrotech.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    /** Interaccion de negocio: registrar una venta. */
    @PostMapping
    public ResponseEntity<PedidoResponse> registrarVenta(@Valid @RequestBody PedidoRequest solicitud) {
        PedidoResponse respuesta = PedidoResponse.desde(pedidoService.registrarVenta(solicitud));
        return ResponseEntity
                .created(URI.create("/api/pedidos/" + respuesta.id()))
                .body(respuesta);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> consultar(@PathVariable("id") Long id) {
        return ResponseEntity.ok(PedidoResponse.desde(pedidoService.consultar(id)));
    }
}
