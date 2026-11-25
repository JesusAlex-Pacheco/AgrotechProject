package edu.itm.agrotech.controller;

import edu.itm.agrotech.dto.AgricultorResponse;
import edu.itm.agrotech.dto.ClienteResponse;
import edu.itm.agrotech.dto.RegistroAgricultorRequest;
import edu.itm.agrotech.dto.RegistroClienteRequest;
import edu.itm.agrotech.dto.RegistroTransportadorRequest;
import edu.itm.agrotech.dto.TransportadorResponse;
import edu.itm.agrotech.security.ControlAcceso;
import edu.itm.agrotech.service.UsuarioService;
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

/** Registro y consulta de los actores del sistema: agricultores, clientes y transportadores. */
@RestController
@RequestMapping("/api")
@Tag(name = "Usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final ControlAcceso acceso;

    public UsuarioController(UsuarioService usuarioService, ControlAcceso acceso) {
        this.usuarioService = usuarioService;
        this.acceso = acceso;
    }

    @PostMapping("/agricultores")
    @Operation(summary = "Registrar un agricultor",
            description = "Crea el usuario con rol AGRICULTOR y su finca en una sola transaccion. "
                    + "La clave se guarda cifrada con BCrypt.")
    @ApiResponse(responseCode = "201", description = "Agricultor registrado")
    @ApiResponse(responseCode = "400", description = "Datos invalidos")
    @ApiResponse(responseCode = "409", description = "El correo ya esta registrado")
    public ResponseEntity<AgricultorResponse> registrarAgricultor(
            @Valid @RequestBody RegistroAgricultorRequest solicitud) {
        AgricultorResponse respuesta = AgricultorResponse.desde(usuarioService.registrarAgricultor(solicitud));
        return ResponseEntity.created(URI.create("/api/agricultores/" + respuesta.id())).body(respuesta);
    }

    @GetMapping("/agricultores/{id}")
    @Operation(summary = "Consultar un agricultor")
    @ApiResponse(responseCode = "200", description = "Agricultor encontrado")
    @ApiResponse(responseCode = "404", description = "El agricultor no existe")
    public ResponseEntity<AgricultorResponse> consultarAgricultor(
            @Parameter(description = "Id del agricultor", example = "1") @PathVariable("id") Long id) {
        return ResponseEntity.ok(AgricultorResponse.desde(usuarioService.consultarAgricultor(id)));
    }

    @PostMapping("/clientes")
    @Operation(summary = "Registrar un cliente",
            description = "Crea el usuario con rol CLIENTE. La direccion se usa por defecto en sus entregas.")
    @ApiResponse(responseCode = "201", description = "Cliente registrado")
    @ApiResponse(responseCode = "400", description = "Datos invalidos")
    @ApiResponse(responseCode = "409", description = "El correo ya esta registrado")
    public ResponseEntity<ClienteResponse> registrarCliente(@Valid @RequestBody RegistroClienteRequest solicitud) {
        ClienteResponse respuesta = ClienteResponse.desde(usuarioService.registrarCliente(solicitud));
        return ResponseEntity.created(URI.create("/api/clientes/" + respuesta.id())).body(respuesta);
    }

    @GetMapping("/clientes/{id}")
    @Operation(summary = "Consultar un cliente", description = "Lo consulta el propio cliente o el ADMIN.")
    @ApiResponse(responseCode = "200", description = "Cliente encontrado")
    @ApiResponse(responseCode = "401", description = "No hay sesion")
    @ApiResponse(responseCode = "403", description = "Es la cuenta de otro cliente")
    @ApiResponse(responseCode = "404", description = "El cliente no existe")
    public ResponseEntity<ClienteResponse> consultarCliente(
            @Parameter(description = "Id del cliente", example = "1") @PathVariable("id") Long id) {
        acceso.exigirClienteOAdmin(id);
        return ResponseEntity.ok(ClienteResponse.desde(usuarioService.consultarCliente(id)));
    }

    @PostMapping("/transportadores")
    @Operation(summary = "Registrar un transportador",
            description = "Crea el usuario con rol TRANSPORTADOR y su vehiculo. La placa se guarda en mayusculas.")
    @ApiResponse(responseCode = "201", description = "Transportador registrado")
    @ApiResponse(responseCode = "400", description = "Datos invalidos")
    @ApiResponse(responseCode = "409", description = "El correo ya esta registrado")
    public ResponseEntity<TransportadorResponse> registrarTransportador(
            @Valid @RequestBody RegistroTransportadorRequest solicitud) {
        TransportadorResponse respuesta =
                TransportadorResponse.desde(usuarioService.registrarTransportador(solicitud));
        return ResponseEntity.created(URI.create("/api/transportadores/" + respuesta.id())).body(respuesta);
    }

    @GetMapping("/transportadores")
    @Operation(summary = "Listar los transportadores",
            description = "En orden alfabetico, para asignarlos a una entrega. Solo AGRICULTOR o ADMIN.")
    @ApiResponse(responseCode = "200", description = "Transportadores (puede estar vacia)")
    @ApiResponse(responseCode = "401", description = "No hay sesion")
    @ApiResponse(responseCode = "403", description = "No es agricultor ni administrador")
    public ResponseEntity<List<TransportadorResponse>> listarTransportadores() {
        return ResponseEntity.ok(usuarioService.listarTransportadores()
                .stream()
                .map(TransportadorResponse::desde)
                .toList());
    }

    @GetMapping("/transportadores/{id}")
    @Operation(summary = "Consultar un transportador", description = "Requiere sesion.")
    @ApiResponse(responseCode = "200", description = "Transportador encontrado")
    @ApiResponse(responseCode = "401", description = "No hay sesion")
    @ApiResponse(responseCode = "404", description = "El transportador no existe")
    public ResponseEntity<TransportadorResponse> consultarTransportador(
            @Parameter(description = "Id del transportador", example = "1") @PathVariable("id") Long id) {
        return ResponseEntity.ok(TransportadorResponse.desde(usuarioService.consultarTransportador(id)));
    }
}
