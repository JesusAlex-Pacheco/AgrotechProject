package edu.itm.agrotech.controller;

import edu.itm.agrotech.dto.ProductoRequest;
import edu.itm.agrotech.dto.ProductoResponse;
import edu.itm.agrotech.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Capa de presentacion: expone el CRUD de Producto como servicios REST.
 * No contiene reglas de negocio ni acceso a datos.
 */
@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    /** CREATE - POST /api/productos */
    @PostMapping
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest solicitud) {
        ProductoResponse creado = ProductoResponse.desde(productoService.crear(solicitud));
        return ResponseEntity
                .created(URI.create("/api/productos/" + creado.id()))
                .body(creado);
    }

    /** READ - GET /api/productos/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponse> consultar(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ProductoResponse.desde(productoService.consultar(id)));
    }

    /** LIST - GET /api/productos?idCategoria=1&nombre=aguacate */
    @GetMapping
    public ResponseEntity<List<ProductoResponse>> listar(
            @RequestParam(name = "idCategoria", required = false) Long idCategoria,
            @RequestParam(name = "nombre", required = false) String nombre) {

        List<ProductoResponse> productos = productoService.listar(idCategoria, nombre)
                .stream()
                .map(ProductoResponse::desde)
                .toList();

        return ResponseEntity.ok(productos);
    }

    /** UPDATE - PUT /api/productos/{id} */
    @PutMapping("/{id}")
    public ResponseEntity<ProductoResponse> actualizar(@PathVariable("id") Long id,
                                                       @Valid @RequestBody ProductoRequest solicitud) {
        return ResponseEntity.ok(ProductoResponse.desde(productoService.actualizar(id, solicitud)));
    }

    /** DELETE - DELETE /api/productos/{id} (baja logica) */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable("id") Long id) {
        productoService.desactivar(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
