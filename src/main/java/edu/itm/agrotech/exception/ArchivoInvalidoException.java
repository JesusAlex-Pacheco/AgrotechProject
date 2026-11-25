package edu.itm.agrotech.exception;

/** El archivo enviado no se acepta (vacio o con un formato no permitido). Se responde 400. */
public class ArchivoInvalidoException extends RuntimeException {

    public ArchivoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
