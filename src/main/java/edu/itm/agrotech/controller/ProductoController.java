package edu.itm.agrotech.controller;

import edu.itm.agrotech.dto.ProductoRequest;
import edu.itm.agrotech.dto.ProductoResponse;
import edu.itm.agrotech.security.ControlAcceso;
import edu.itm.agrotech.service.FotoProductoService;
import edu.itm.agrotech.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
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
    private final FotoProductoService fotoProductoService;
    private final ControlAcceso acceso;

    public ProductoController(ProductoService productoService, FotoProductoService fotoProductoService,
                              ControlAcceso acceso) {
        this.productoService = productoService;
        this.fotoProductoService = fotoProductoService;
        this.acceso = acceso;
    }

    @PutMapping(value = "/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir la foto de un producto",
            description = "Imagen JPG, PNG o WEBP de hasta 5 MB en el campo 'archivo' (multipart/form-data). "
                    + "Reemplaza la foto anterior. Queda publicada en la ruta que devuelve fotoUrl. "
                    + "Solo el agricultor que publico el producto o el ADMIN.")
    @ApiResponse(responseCode = "200", description = "Foto guardada")
    @ApiResponse(responseCode = "400", description = "Falta el archivo o no es una imagen JPG, PNG o WEBP")
    @ApiResponse(responseCode = "401", description = "No hay sesion")
    @ApiResponse(responseCode = "403", description = "El producto es de otro agricultor")
    @ApiResponse(responseCode = "404", description = "El producto no existe")
    @ApiResponse(responseCode = "413", description = "La foto pesa mas de 5 MB")
    public ResponseEntity<ProductoResponse> subirFoto(
            @Parameter(description = "Id del producto", example = "1") @PathVariable("id") Long id,
            @RequestPart("archivo") MultipartFile archivo) throws IOException {
        acceso.exigirDuenoDelProducto(id);
        return ResponseEntity.ok(ProductoResponse.desde(fotoProductoService.guardarFoto(id, archivo.getBytes())));
    }

    @DeleteMapping("/{id}/foto")
    @Operation(summary = "Quitar la foto de un producto",
            description = "Solo el agricultor que publico el producto o el ADMIN.")
    @ApiResponse(responseCode = "200", description = "Foto quitada")
    @ApiResponse(responseCode = "401", description = "No hay sesion")
    @ApiResponse(responseCode = "403", description = "El producto es de otro agricultor")
    @ApiResponse(responseCode = "404", description = "El producto no existe")
    public ResponseEntity<ProductoResponse> quitarFoto(
            @Parameter(description = "Id del producto", example = "1") @PathVariable("id") Long id) {
        acceso.exigirDuenoDelProducto(id);
        return ResponseEntity.ok(ProductoResponse.desde(fotoProductoService.quitarFoto(id)));
    }

    @PostMapping
    @Operation(summary = "Publicar un producto",
            description = "El agricultor y la categoria deben existir. Con stock 0 el producto queda no disponible. "
                    + "Requiere sesion de AGRICULTOR (a nombre de su finca) o de ADMIN.")
    @ApiResponse(responseCode = "201", description = "Producto creado")
    @ApiResponse(responseCode = "400", description = "Datos invalidos")
    @ApiResponse(responseCode = "401", description = "No hay sesion")
    @ApiResponse(responseCode = "403", description = "No es agricultor o publica a nombre de otro")
    @ApiResponse(responseCode = "404", description = "El agricultor o la categoria no existen")
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest solicitud) {
        acceso.exigirAgricultorOAdmin(solicitud.idAgricultor());
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
            description = "Se envian todos los campos. Un producto no puede cambiar de agricultor. "
                    + "Solo el agricultor que lo publico o el ADMIN.")
    @ApiResponse(responseCode = "200", description = "Producto actualizado")
    @ApiResponse(responseCode = "400", description = "Datos invalidos")
    @ApiResponse(responseCode = "401", description = "No hay sesion")
    @ApiResponse(responseCode = "403", description = "El producto es de otro agricultor")
    @ApiResponse(responseCode = "404", description = "El producto o la categoria no existen")
    @ApiResponse(responseCode = "409", description = "Se intento cambiar el agricultor")
    public ResponseEntity<ProductoResponse> actualizar(
            @Parameter(description = "Id del producto", example = "1") @PathVariable("id") Long id,
            @Valid @RequestBody ProductoRequest solicitud) {
        acceso.exigirDuenoDelProducto(id);
        return ResponseEntity.ok(ProductoResponse.desde(productoService.actualizar(id, solicitud)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Retirar un producto del catalogo",
            description = "Baja logica: marca disponible = false sin borrar la fila, para conservar los pedidos "
                    + "historicos. Solo el agricultor que lo publico o el ADMIN.")
    @ApiResponse(responseCode = "204", description = "Producto retirado")
    @ApiResponse(responseCode = "401", description = "No hay sesion")
    @ApiResponse(responseCode = "403", description = "El producto es de otro agricultor")
    @ApiResponse(responseCode = "404", description = "El producto no existe")
    public ResponseEntity<Void> desactivar(
            @Parameter(description = "Id del producto", example = "3") @PathVariable("id") Long id) {
        acceso.exigirDuenoDelProducto(id);
        productoService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
