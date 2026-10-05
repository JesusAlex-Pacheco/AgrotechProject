package edu.itm.agrotech.controller;

import edu.itm.agrotech.dto.CategoriaRequest;
import edu.itm.agrotech.dto.CategoriaResponse;
import edu.itm.agrotech.service.CategoriaService;
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
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Capa de presentacion: expone el CRUD de Categoria como servicios REST.
 * No contiene reglas de negocio ni acceso a datos.
 */
@RestController
@RequestMapping("/api/categorias")
@Tag(name = "Categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @PostMapping
    @Operation(summary = "Crear una categoria", description = "El nombre no se puede repetir, sin importar mayusculas.")
    @ApiResponse(responseCode = "201", description = "Categoria creada")
    @ApiResponse(responseCode = "400", description = "Datos invalidos")
    @ApiResponse(responseCode = "409", description = "Ya existe una categoria con ese nombre")
    public ResponseEntity<CategoriaResponse> crear(@Valid @RequestBody CategoriaRequest solicitud) {
        CategoriaResponse creada = CategoriaResponse.desde(categoriaService.crear(solicitud));
        return ResponseEntity
                .created(URI.create("/api/categorias/" + creada.id()))
                .body(creada);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar una categoria")
    @ApiResponse(responseCode = "200", description = "Categoria encontrada")
    @ApiResponse(responseCode = "404", description = "La categoria no existe")
    public ResponseEntity<CategoriaResponse> consultar(
            @Parameter(description = "Id de la categoria", example = "1") @PathVariable("id") Long id) {
        return ResponseEntity.ok(CategoriaResponse.desde(categoriaService.consultar(id)));
    }

    @GetMapping
    @Operation(summary = "Listar las categorias", description = "En orden alfabetico.")
    @ApiResponse(responseCode = "200", description = "Categorias")
    public ResponseEntity<List<CategoriaResponse>> listar() {
        List<CategoriaResponse> categorias = categoriaService.listar()
                .stream()
                .map(CategoriaResponse::desde)
                .toList();

        return ResponseEntity.ok(categorias);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Renombrar una categoria")
    @ApiResponse(responseCode = "200", description = "Categoria actualizada")
    @ApiResponse(responseCode = "400", description = "Datos invalidos")
    @ApiResponse(responseCode = "404", description = "La categoria no existe")
    @ApiResponse(responseCode = "409", description = "El nombre ya lo usa otra categoria")
    public ResponseEntity<CategoriaResponse> actualizar(
            @Parameter(description = "Id de la categoria", example = "1") @PathVariable("id") Long id,
            @Valid @RequestBody CategoriaRequest solicitud) {
        return ResponseEntity.ok(CategoriaResponse.desde(categoriaService.actualizar(id, solicitud)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una categoria",
            description = "Borrado fisico. No se permite si la categoria tiene productos asociados.")
    @ApiResponse(responseCode = "204", description = "Categoria eliminada")
    @ApiResponse(responseCode = "404", description = "La categoria no existe")
    @ApiResponse(responseCode = "409", description = "La categoria tiene productos")
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "Id de la categoria", example = "5") @PathVariable("id") Long id) {
        categoriaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
