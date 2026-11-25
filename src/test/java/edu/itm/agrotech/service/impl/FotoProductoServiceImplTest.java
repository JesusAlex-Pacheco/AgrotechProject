package edu.itm.agrotech.service.impl;

import edu.itm.agrotech.domain.Producto;
import edu.itm.agrotech.exception.ArchivoInvalidoException;
import edu.itm.agrotech.exception.RecursoNoEncontradoException;
import edu.itm.agrotech.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FotoProductoServiceImplTest {

    // Primeros bytes reales de cada formato, completados hasta 12 bytes
    private static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F', 0, 1};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};

    @TempDir
    private Path carpeta;

    @Mock
    private ProductoRepository productoRepository;

    private FotoProductoServiceImpl servicio;
    private Producto producto;

    @BeforeEach
    void preparar() {
        servicio = new FotoProductoServiceImpl(productoRepository, carpeta.toString());
        producto = new Producto();
        producto.setId(5L);
    }

    private Path archivoDe(String url) {
        return carpeta.resolve(url.substring(FotoProductoServiceImpl.RUTA_PUBLICA.length()));
    }

    @Test
    @DisplayName("Guarda el archivo en la carpeta y la ruta publica en el producto")
    void guarda() throws IOException {
        when(productoRepository.buscarPorId(5L)).thenReturn(Optional.of(producto));

        Producto resultado = servicio.guardarFoto(5L, JPG);

        assertThat(resultado.getFotoUrl()).startsWith("/api/fotos/producto-5-").endsWith(".jpg");
        assertThat(Files.readAllBytes(archivoDe(resultado.getFotoUrl()))).isEqualTo(JPG);
        verify(productoRepository).actualizar(producto);
    }

    @Test
    @DisplayName("Reconoce JPG, PNG y WEBP por su contenido")
    void formatos() {
        assertThat(FotoProductoServiceImpl.extensionDeImagen(JPG)).contains("jpg");
        assertThat(FotoProductoServiceImpl.extensionDeImagen(PNG)).contains("png");
        assertThat(FotoProductoServiceImpl.extensionDeImagen(WEBP)).contains("webp");
    }

    @Test
    @DisplayName("Rechaza un archivo que no es imagen aunque se llame .jpg")
    void rechazaNoImagen() {
        when(productoRepository.buscarPorId(5L)).thenReturn(Optional.of(producto));
        byte[] texto = "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> servicio.guardarFoto(5L, texto))
                .isInstanceOf(ArchivoInvalidoException.class);
        verify(productoRepository, never()).actualizar(any());
        assertThat(carpeta.toFile().list()).isEmpty();
    }

    @Test
    @DisplayName("Al reemplazar la foto, borra el archivo anterior")
    void reemplaza() {
        when(productoRepository.buscarPorId(5L)).thenReturn(Optional.of(producto));
        String primera = servicio.guardarFoto(5L, JPG).getFotoUrl();

        String segunda = servicio.guardarFoto(5L, PNG).getFotoUrl();

        assertThat(archivoDe(primera)).doesNotExist();
        assertThat(archivoDe(segunda)).exists();
    }

    @Test
    @DisplayName("Quitar la foto deja fotoUrl en null y borra el archivo")
    void quita() {
        when(productoRepository.buscarPorId(5L)).thenReturn(Optional.of(producto));
        String url = servicio.guardarFoto(5L, WEBP).getFotoUrl();

        Producto resultado = servicio.quitarFoto(5L);

        assertThat(resultado.getFotoUrl()).isNull();
        assertThat(archivoDe(url)).doesNotExist();
    }

    @Test
    @DisplayName("Un enlace externo no se intenta borrar como archivo")
    void enlaceExterno() {
        producto.setFotoUrl("https://ejemplo.com/../../secreto.txt");
        when(productoRepository.buscarPorId(5L)).thenReturn(Optional.of(producto));

        assertThat(servicio.quitarFoto(5L).getFotoUrl()).isNull();
    }

    @Test
    @DisplayName("Producto inexistente: 404 y no se escribe nada")
    void productoInexistente() {
        when(productoRepository.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.guardarFoto(99L, JPG)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThat(carpeta.toFile().list()).isEmpty();
    }
}
