package edu.itm.agrotech.exception;

import org.springframework.security.access.AccessDeniedException;

/**
 * El usuario inicio sesion, pero no puede hacer esta operacion: no tiene el
 * rol o el recurso no es suyo (un producto de otro agricultor, el pedido de
 * otro cliente...). Se responde con 403 y el mensaje explica el motivo.
 */
public class AccesoDenegadoException extends AccessDeniedException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
