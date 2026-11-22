package edu.itm.agrotech.exception;

/**
 * Correo o clave incorrectos al iniciar sesion. El mensaje es el mismo en
 * ambos casos para no revelar que correos estan registrados.
 */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        super("Correo o clave incorrectos");
    }
}
