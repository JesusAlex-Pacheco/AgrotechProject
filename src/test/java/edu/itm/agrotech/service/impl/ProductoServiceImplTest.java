package edu.itm.agrotech.service.impl;

import edu.itm.agrotech.domain.Categoria;
import edu.itm.agrotech.domain.Producto;
import edu.itm.agrotech.dto.ProductoRequest;
import edu.itm.agrotech.exception.RecursoNoEncontradoException;
import edu.itm.agrotech.exception.ReglaNegocioException;
import edu.itm.agrotech.repository.AgricultorRepository;
import edu.itm.agrotech.repository.CategoriaRepository;
import edu.itm.agrotech.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoServiceImplTest {

    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private AgricultorRepository agricultorRepository;

    private ProductoServiceImpl servicio;

    @BeforeEach
    void preparar() {
        servicio = new ProductoServiceImpl(productoRepository, categoriaRepository, agricultorRepository);
    }

    @Test
    @DisplayName("Crear: rechaza un agricultor que no existe")
    void crearConAgricultorInexistente() {
        when(agricultorRepository.existe(99L)).thenReturn(false);

        assertThatThrownBy(() -> servicio.crear(solicitud(99L, 1L, "50")))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No existe el agricultor con id 99");
        verify(productoRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("Crear: rechaza una categoria que no existe")
    void crearConCategoriaInexistente() {
        when(agricultorRepository.existe(1L)).thenReturn(true);
        when(categoriaRepository.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.crear(solicitud(1L, 99L, "50")))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No existe la categoria con id 99");
    }

    @Test
    @DisplayName("Crear: con stock 0 el producto nace no disponible")
    void crearSinExistencias() {
        when(agricultorRepository.existe(1L)).thenReturn(true);
        when(categoriaRepository.buscarPorId(1L)).thenReturn(Optional.of(new Categoria()));
        when(productoRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        Producto creado = servicio.crear(solicitud(1L, 1L, "0"));

        assertThat(creado.isDisponible()).isFalse();
        assertThat(creado.getNombre()).isEqualTo("Mango");
    }

    @Test
    @DisplayName("Actualizar: un producto no puede cambiar de agricultor")
    void actualizarCambiandoAgricultor() {
        when(productoRepository.buscarPorId(1L)).thenReturn(Optional.of(productoDelAgricultor(1L)));

        assertThatThrownBy(() -> servicio.actualizar(1L, solicitud(2L, 1L, "10")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("no puede cambiar de agricultor");
        verify(productoRepository, never()).actualizar(any());
    }

    @Test
    @DisplayName("Consultar: un producto inexistente lanza 404")
    void consultarInexistente() {
        when(productoRepository.buscarPorId(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.consultar(999L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No existe el producto con id 999");
    }

    private static ProductoRequest solicitud(Long idAgricultor, Long idCategoria, String stock) {
        return new ProductoRequest(idAgricultor, idCategoria, "  Mango  ", null,
                new BigDecimal("4200"), "kg", new BigDecimal(stock), null);
    }

    private static Producto productoDelAgricultor(Long idAgricultor) {
        Producto producto = new Producto();
        producto.setId(1L);
        producto.setIdAgricultor(idAgricultor);
        return producto;
    }
}
