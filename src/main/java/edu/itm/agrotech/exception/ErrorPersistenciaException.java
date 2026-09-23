package edu.itm.agrotech.exception;

public class ErrorPersistenciaException extends RuntimeException {

    public ErrorPersistenciaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
