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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

    @Nested
    @DisplayName("Crear")
    class Crear {

        @Test
        @DisplayName("Guarda el producto con el nombre limpio y disponible si tiene stock")
        void crea() {
            when(agricultorRepository.existsById(1L)).thenReturn(true);
            when(categoriaRepository.buscarPorId(1L)).thenReturn(Optional.of(new Categoria()));
            when(productoRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

            Producto creado = servicio.crear(solicitud(1L, 1L, "50"));

            assertThat(creado.getNombre()).isEqualTo("Mango");
            assertThat(creado.getIdAgricultor()).isEqualTo(1L);
            assertThat(creado.isDisponible()).isTrue();
        }

        @Test
        @DisplayName("Con stock 0 el producto nace no disponible")
        void sinExistencias() {
            when(agricultorRepository.existsById(1L)).thenReturn(true);
            when(categoriaRepository.buscarPorId(1L)).thenReturn(Optional.of(new Categoria()));
            when(productoRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThat(servicio.crear(solicitud(1L, 1L, "0")).isDisponible()).isFalse();
        }

        @Test
        @DisplayName("Rechaza un agricultor que no existe")
        void agricultorInexistente() {
            when(agricultorRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> servicio.crear(solicitud(99L, 1L, "50")))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessage("No existe el agricultor con id 99");
            verify(productoRepository, never()).guardar(any());
        }

        @Test
        @DisplayName("Rechaza una categoria que no existe")
        void categoriaInexistente() {
            when(agricultorRepository.existsById(1L)).thenReturn(true);
            when(categoriaRepository.buscarPorId(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.crear(solicitud(1L, 99L, "50")))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessage("No existe la categoria con id 99");
        }
    }

    @Nested
    @DisplayName("Consultar y listar")
    class ConsultarYListar {

        @Test
        @DisplayName("Consultar devuelve el producto encontrado")
        void consulta() {
            Producto producto = productoDelAgricultor(1L);
            when(productoRepository.buscarPorId(1L)).thenReturn(Optional.of(producto));

            assertThat(servicio.consultar(1L)).isSameAs(producto);
        }

        @Test
        @DisplayName("Consultar un producto inexistente lanza 404")
        void consultarInexistente() {
            when(productoRepository.buscarPorId(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.consultar(999L))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessage("No existe el producto con id 999");
        }

        @Test
        @DisplayName("Listar delega los filtros al repositorio")
        void lista() {
            List<Producto> catalogo = List.of(productoDelAgricultor(1L));
            when(productoRepository.listar(1L, "agua")).thenReturn(catalogo);

            assertThat(servicio.listar(1L, "agua")).isSameAs(catalogo);
        }
    }

    @Nested
    @DisplayName("Actualizar")
    class Actualizar {

        @Test
        @DisplayName("Aplica los datos nuevos y guarda")
        void actualiza() {
            Producto producto = productoDelAgricultor(1L);
            when(productoRepository.buscarPorId(1L)).thenReturn(Optional.of(producto));
            when(categoriaRepository.buscarPorId(2L)).thenReturn(Optional.of(new Categoria()));

            Producto actualizado = servicio.actualizar(1L, solicitud(1L, 2L, "10"));

            assertThat(actualizado.getIdCategoria()).isEqualTo(2L);
            assertThat(actualizado.getStock()).isEqualByComparingTo("10");
            verify(productoRepository).actualizar(producto);
        }

        @Test
        @DisplayName("Un producto no puede cambiar de agricultor")
        void cambiandoAgricultor() {
            when(productoRepository.buscarPorId(1L)).thenReturn(Optional.of(productoDelAgricultor(1L)));

            assertThatThrownBy(() -> servicio.actualizar(1L, solicitud(2L, 1L, "10")))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("no puede cambiar de agricultor");
            verify(productoRepository, never()).actualizar(any());
        }

        @Test
        @DisplayName("Un producto inexistente lanza 404")
        void inexistente() {
            when(productoRepository.buscarPorId(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.actualizar(999L, solicitud(1L, 1L, "10")))
                    .isInstanceOf(RecursoNoEncontradoException.class);
            verify(productoRepository, never()).actualizar(any());
        }
    }

    @Nested
    @DisplayName("Desactivar")
    class Desactivar {

        @Test
        @DisplayName("Hace la baja logica del producto")
        void desactiva() {
            when(productoRepository.buscarPorId(3L)).thenReturn(Optional.of(productoDelAgricultor(1L)));

            servicio.desactivar(3L);

            verify(productoRepository).desactivar(3L);
        }

        @Test
        @DisplayName("Un producto inexistente lanza 404")
        void inexistente() {
            when(productoRepository.buscarPorId(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.desactivar(999L))
                    .isInstanceOf(RecursoNoEncontradoException.class);
            verify(productoRepository, never()).desactivar(anyLong());
        }
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
