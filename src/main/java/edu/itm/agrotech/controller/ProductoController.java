package edu.itm.agrotech.controller;

import edu.itm.agrotech.dto.ProductoRequest;
import edu.itm.agrotech.dto.ProductoResponse;
import edu.itm.agrotech.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@Tag(name = "Productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @PostMapping
    @Operation(summary = "Publicar un producto",
            description = "El agricultor y la categoria deben existir. Con stock 0 el producto queda no disponible.")
    @ApiResponse(responseCode = "201", description = "Producto creado")
    @ApiResponse(responseCode = "400", description = "Datos invalidos")
    @ApiResponse(responseCode = "404", description = "El agricultor o la categoria no existen")
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest solicitud) {
        ProductoResponse creado = ProductoResponse.desde(productoService.crear(solicitud));
        return ResponseEntity
                .created(URI.create("/api/productos/" + creado.id()))
                .body(creado);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un producto")
    @ApiResponse(responseCode = "200", description = "Producto encontrado")
    @ApiResponse(responseCode = "404", description = "El producto no existe")
    public ResponseEntity<ProductoResponse> consultar(
            @Parameter(description = "Id del producto", example = "1") @PathVariable("id") Long id) {
        return ResponseEntity.ok(ProductoResponse.desde(productoService.consultar(id)));
    }

    @GetMapping
    @Operation(summary = "Listar el catalogo", description = "Filtros opcionales por categoria y por parte del nombre.")
    @ApiResponse(responseCode = "200", description = "Catalogo (puede estar vacio)")
    public ResponseEntity<List<ProductoResponse>> listar(
            @Parameter(description = "Id de la categoria", example = "1")
            @RequestParam(name = "idCategoria", required = false) Long idCategoria,
            @Parameter(description = "Parte del nombre, sin distinguir mayusculas", example = "agua")
            @RequestParam(name = "nombre", required = false) String nombre) {

        List<ProductoResponse> productos = productoService.listar(idCategoria, nombre)
                .stream()
                .map(ProductoResponse::desde)
                .toList();

        return ResponseEntity.ok(productos);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un producto",
            description = "Se envian todos los campos. Un producto no puede cambiar de agricultor.")
    @ApiResponse(responseCode = "200", description = "Producto actualizado")
    @ApiResponse(responseCode = "400", description = "Datos invalidos")
    @ApiResponse(responseCode = "404", description = "El producto o la categoria no existen")
    @ApiResponse(responseCode = "409", description = "Se intento cambiar el agricultor")
    public ResponseEntity<ProductoResponse> actualizar(
            @Parameter(description = "Id del producto", example = "1") @PathVariable("id") Long id,
            @Valid @RequestBody ProductoRequest solicitud) {
        return ResponseEntity.ok(ProductoResponse.desde(productoService.actualizar(id, solicitud)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Retirar un producto del catalogo",
            description = "Baja logica: marca disponible = false sin borrar la fila, para conservar los pedidos historicos.")
    @ApiResponse(responseCode = "204", description = "Producto retirado")
    @ApiResponse(responseCode = "404", description = "El producto no existe")
    public ResponseEntity<Void> desactivar(
            @Parameter(description = "Id del producto", example = "3") @PathVariable("id") Long id) {
        productoService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
