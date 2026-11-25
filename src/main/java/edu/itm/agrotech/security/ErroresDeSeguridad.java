package edu.itm.agrotech.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Los rechazos de Spring Security ocurren en los filtros, antes de llegar a
 * los controladores, asi que el {@code @RestControllerAdvice} no los ve.
 * Esta clase se los entrega a Spring MVC para que ManejadorGlobalErrores
 * responda 401 o 403 con el mismo formato ErrorResponse que el resto de la API.
 */
@Component
public class ErroresDeSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final HandlerExceptionResolver resolutor;

    public ErroresDeSeguridad(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolutor) {
        this.resolutor = resolutor;
    }

    /** Sin token, o con un token vencido o alterado: 401. */
    @Override
    public void commence(HttpServletRequest solicitud, HttpServletResponse respuesta,
                         AuthenticationException excepcion) {
        resolutor.resolveException(solicitud, respuesta, null, excepcion);
    }

    /** Con sesion, pero sin el rol que exige la ruta: 403. */
    @Override
    public void handle(HttpServletRequest solicitud, HttpServletResponse respuesta,
                       AccessDeniedException excepcion) {
        resolutor.resolveException(solicitud, respuesta, null, excepcion);
    }
}
