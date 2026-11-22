package edu.itm.agrotech.controller;

import edu.itm.agrotech.dto.InicioSesionRequest;
import edu.itm.agrotech.domain.SesionUsuario;
import edu.itm.agrotech.dto.SesionResponse;
import edu.itm.agrotech.security.ServicioTokens;
import edu.itm.agrotech.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Inicio de sesion de los usuarios registrados. */
@RestController
@RequestMapping("/api/sesiones")
@Tag(name = "Usuarios")
public class SesionController {

    private final UsuarioService usuarioService;
    private final ServicioTokens servicioTokens;

    public SesionController(UsuarioService usuarioService, ServicioTokens servicioTokens) {
        this.usuarioService = usuarioService;
        this.servicioTokens = servicioTokens;
    }

    @PostMapping
    @Operation(summary = "Iniciar sesion",
            description = "Verifica el correo y la clave (BCrypt) y devuelve los datos del usuario, "
                    + "el id de su perfil segun el rol y un token de sesion que vence en 8 horas. "
                    + "Las rutas protegidas exigen ese token en la cabecera 'Authorization: Bearer <token>' "
                    + "(en Swagger, boton Authorize). No devuelve la clave ni su hash.")
    @ApiResponse(responseCode = "200", description = "Sesion iniciada")
    @ApiResponse(responseCode = "400", description = "Falta el correo o la clave")
    @ApiResponse(responseCode = "401", description = "Correo o clave incorrectos")
    public ResponseEntity<SesionResponse> iniciarSesion(@Valid @RequestBody InicioSesionRequest solicitud) {
        SesionUsuario sesion = usuarioService.iniciarSesion(solicitud.correo(), solicitud.clave());
        return ResponseEntity.ok(SesionResponse.desde(sesion, servicioTokens.emitir(sesion)));
    }
}
