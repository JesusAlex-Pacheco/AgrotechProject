package edu.itm.agrotech.controller;

import edu.itm.agrotech.dto.CategoriaRequest;
import edu.itm.agrotech.dto.CategoriaResponse;
import edu.itm.agrotech.service.CategoriaService;
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
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Capa de presentacion: expone el CRUD de Categoria como servicios REST.
 * No contiene reglas de negocio ni acceso a datos.
 */
@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    /** CREATE - POST /api/categorias */
    @PostMapping
    public ResponseEntity<CategoriaResponse> crear(@Valid @RequestBody CategoriaRequest solicitud) {
        CategoriaResponse creada = CategoriaResponse.desde(categoriaService.crear(solicitud));
        return ResponseEntity
                .created(URI.create("/api/categorias/" + creada.id()))
                .body(creada);
    }

    /** READ - GET /api/categorias/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<CategoriaResponse> consultar(@PathVariable("id") Long id) {
        return ResponseEntity.ok(CategoriaResponse.desde(categoriaService.consultar(id)));
    }

    /** LIST - GET /api/categorias */
    @GetMapping
    public ResponseEntity<List<CategoriaResponse>> listar() {
        List<CategoriaResponse> categorias = categoriaService.listar()
                .stream()
                .map(CategoriaResponse::desde)
                .toList();

        return ResponseEntity.ok(categorias);
    }

    /** UPDATE - PUT /api/categorias/{id} */
    @PutMapping("/{id}")
    public ResponseEntity<CategoriaResponse> actualizar(@PathVariable("id") Long id,
                                                        @Valid @RequestBody CategoriaRequest solicitud) {
        return ResponseEntity.ok(CategoriaResponse.desde(categoriaService.actualizar(id, solicitud)));
    }

    /** DELETE - DELETE /api/categorias/{id} (solo si no tiene productos) */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable("id") Long id) {
        categoriaService.eliminar(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
