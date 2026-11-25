package edu.itm.agrotech.service.impl;

import edu.itm.agrotech.domain.Producto;
import edu.itm.agrotech.exception.ArchivoInvalidoException;
import edu.itm.agrotech.exception.ErrorPersistenciaException;
import edu.itm.agrotech.exception.RecursoNoEncontradoException;
import edu.itm.agrotech.repository.ProductoRepository;
import edu.itm.agrotech.service.FotoProductoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

/**
 * Guarda las fotos como archivos en una carpeta del servidor y en la base
 * solo la ruta publica (/api/fotos/archivo), que FotosConfig sirve.
 *
 * <p>Seguridad: el formato se reconoce por los primeros bytes del archivo
 * (no por su nombre ni por lo que diga el navegador), y el nombre lo genera
 * el servidor, de modo que nadie puede escribir fuera de la carpeta.
 */
@Service
public class FotoProductoServiceImpl implements FotoProductoService {

    public static final String RUTA_PUBLICA = "/api/fotos/";

    private static final Logger LOG = LoggerFactory.getLogger(FotoProductoServiceImpl.class);

    private final ProductoRepository productoRepository;
    private final Path directorio;

    public FotoProductoServiceImpl(ProductoRepository productoRepository,
                                   @Value("${agrotech.fotos.directorio:fotos}") String directorio) {
        this.productoRepository = productoRepository;
        this.directorio = Path.of(directorio).toAbsolutePath().normalize();
    }

    @Override
    public Producto guardarFoto(Long idProducto, byte[] contenido) {
        Producto producto = consultar(idProducto);
        String extension = extensionDeImagen(contenido)
                .orElseThrow(() -> new ArchivoInvalidoException("La foto debe ser una imagen JPG, PNG o WEBP"));

        String nombre = "producto-" + idProducto + "-" + UUID.randomUUID().toString().substring(0, 8) + "." + extension;
        try {
            Files.createDirectories(directorio);
            Files.write(directorio.resolve(nombre), contenido);
        } catch (IOException e) {
            throw new ErrorPersistenciaException("No fue posible guardar la foto", e);
        }

        String anterior = producto.getFotoUrl();
        producto.setFotoUrl(RUTA_PUBLICA + nombre);
        productoRepository.actualizar(producto);
        borrarArchivo(anterior);
        return producto;
    }

    @Override
    public Producto quitarFoto(Long idProducto) {
        Producto producto = consultar(idProducto);
        String anterior = producto.getFotoUrl();
        producto.setFotoUrl(null);
        productoRepository.actualizar(producto);
        borrarArchivo(anterior);
        return producto;
    }

    private Producto consultar(Long idProducto) {
        return productoRepository.buscarPorId(idProducto)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el producto con id " + idProducto));
    }

    /** Reconoce JPG, PNG y WEBP por su "numero magico" (los primeros bytes del archivo). */
    static Optional<String> extensionDeImagen(byte[] b) {
        if (b == null || b.length < 12) {
            return Optional.empty();
        }
        if ((b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return Optional.of("jpg");
        }
        if ((b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return Optional.of("png");
        }
        if (b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return Optional.of("webp");
        }
        return Optional.empty();
    }

    /** Borra el archivo de una foto anterior, solo si es de esta carpeta (no un enlace externo). */
    private void borrarArchivo(String url) {
        if (url == null || !url.startsWith(RUTA_PUBLICA)) {
            return;
        }
        Path archivo = directorio.resolve(url.substring(RUTA_PUBLICA.length())).normalize();
        if (!archivo.startsWith(directorio)) {
            return;
        }
        try {
            Files.deleteIfExists(archivo);
        } catch (IOException e) {
            // La foto nueva ya quedo guardada: un archivo viejo sin borrar no es grave
            LOG.warn("No se pudo borrar la foto anterior {}: {}", archivo, e.getMessage());
        }
    }
}
