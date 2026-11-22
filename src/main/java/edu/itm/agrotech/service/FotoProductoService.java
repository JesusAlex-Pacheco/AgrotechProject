package edu.itm.agrotech.service;

import edu.itm.agrotech.domain.Producto;

/** Contrato de negocio de la foto de un producto. */
public interface FotoProductoService {

    /**
     * Guarda la imagen como foto del producto y reemplaza la anterior, si habia.
     *
     * @throws edu.itm.agrotech.exception.RecursoNoEncontradoException si el producto no existe
     * @throws edu.itm.agrotech.exception.ArchivoInvalidoException si no es una imagen JPG, PNG o WEBP
     */
    Producto guardarFoto(Long idProducto, byte[] contenido);

    /**
     * Quita la foto del producto y borra su archivo.
     *
     * @throws edu.itm.agrotech.exception.RecursoNoEncontradoException si el producto no existe
     */
    Producto quitarFoto(Long idProducto);
}
