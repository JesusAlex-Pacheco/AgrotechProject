package edu.itm.agrotech.service.impl;

import edu.itm.agrotech.domain.Categoria;
import edu.itm.agrotech.dto.CategoriaRequest;
import edu.itm.agrotech.exception.RecursoNoEncontradoException;
import edu.itm.agrotech.exception.ReglaNegocioException;
import edu.itm.agrotech.repository.CategoriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceImplTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    private CategoriaServiceImpl servicio;

    @BeforeEach
    void preparar() {
        servicio = new CategoriaServiceImpl(categoriaRepository);
    }

    @Nested
    @DisplayName("Crear")
    class Crear {

        @Test
        @DisplayName("Guarda la categoria con el nombre sin espacios sobrantes")
        void crea() {
            when(categoriaRepository.buscarPorNombre("Granos")).thenReturn(Optional.empty());
            when(categoriaRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThat(servicio.crear(new CategoriaRequest("  Granos ")).getNombre()).isEqualTo("Granos");
        }

        @Test
        @DisplayName("Rechaza un nombre que ya existe")
        void nombreRepetido() {
            when(categoriaRepository.buscarPorNombre("frutas")).thenReturn(Optional.of(categoria(1L, "Frutas")));

            assertThatThrownBy(() -> servicio.crear(new CategoriaRequest(" frutas ")))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("Ya existe la categoria 'Frutas'");
            verify(categoriaRepository, never()).guardar(any());
        }
    }

    @Nested
    @DisplayName("Consultar y listar")
    class ConsultarYListar {

        @Test
        @DisplayName("Consultar devuelve la categoria encontrada")
        void consulta() {
            Categoria frutas = categoria(1L, "Frutas");
            when(categoriaRepository.buscarPorId(1L)).thenReturn(Optional.of(frutas));

            assertThat(servicio.consultar(1L)).isSameAs(frutas);
        }

        @Test
        @DisplayName("Consultar una categoria inexistente lanza 404")
        void consultarInexistente() {
            when(categoriaRepository.buscarPorId(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.consultar(99L))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessage("No existe la categoria con id 99");
        }

        @Test
        @DisplayName("Listar devuelve las categorias del repositorio")
        void lista() {
            List<Categoria> categorias = List.of(categoria(1L, "Frutas"), categoria(2L, "Verduras"));
            when(categoriaRepository.listar()).thenReturn(categorias);

            assertThat(servicio.listar()).isSameAs(categorias);
        }
    }

    @Nested
    @DisplayName("Actualizar")
    class Actualizar {

        @Test
        @DisplayName("Puede conservar su propio nombre")
        void conSuMismoNombre() {
            Categoria frutas = categoria(1L, "Frutas");
            when(categoriaRepository.buscarPorId(1L)).thenReturn(Optional.of(frutas));
            when(categoriaRepository.buscarPorNombre("Frutas")).thenReturn(Optional.of(frutas));

            assertThat(servicio.actualizar(1L, new CategoriaRequest("Frutas")).getNombre()).isEqualTo("Frutas");
            verify(categoriaRepository).actualizar(frutas);
        }

        @Test
        @DisplayName("No puede tomar el nombre de otra categoria")
        void conNombreDeOtra() {
            when(categoriaRepository.buscarPorId(2L)).thenReturn(Optional.of(categoria(2L, "Verduras")));
            when(categoriaRepository.buscarPorNombre("Frutas")).thenReturn(Optional.of(categoria(1L, "Frutas")));

            assertThatThrownBy(() -> servicio.actualizar(2L, new CategoriaRequest("Frutas")))
                    .isInstanceOf(ReglaNegocioException.class);
            verify(categoriaRepository, never()).actualizar(any());
        }

        @Test
        @DisplayName("Una categoria inexistente lanza 404")
        void inexistente() {
            when(categoriaRepository.buscarPorId(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.actualizar(99L, new CategoriaRequest("Granos")))
                    .isInstanceOf(RecursoNoEncontradoException.class);
        }
    }

    @Nested
    @DisplayName("Eliminar")
    class Eliminar {

        @Test
        @DisplayName("Sin productos se elimina")
        void sinProductos() {
            when(categoriaRepository.buscarPorId(5L)).thenReturn(Optional.of(categoria(5L, "Granos")));
            when(categoriaRepository.tieneProductos(5L)).thenReturn(false);

            servicio.eliminar(5L);

            verify(categoriaRepository).eliminar(5L);
        }

        @Test
        @DisplayName("No se permite si tiene productos")
        void conProductos() {
            when(categoriaRepository.buscarPorId(1L)).thenReturn(Optional.of(categoria(1L, "Frutas")));
            when(categoriaRepository.tieneProductos(1L)).thenReturn(true);

            assertThatThrownBy(() -> servicio.eliminar(1L))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("tiene productos asociados");
            verify(categoriaRepository, never()).eliminar(any());
        }

        @Test
        @DisplayName("Una categoria inexistente lanza 404")
        void inexistente() {
            when(categoriaRepository.buscarPorId(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.eliminar(99L))
                    .isInstanceOf(RecursoNoEncontradoException.class);
            verify(categoriaRepository, never()).eliminar(any());
        }
    }

    private static Categoria categoria(Long id, String nombre) {
        Categoria categoria = new Categoria();
        categoria.setId(id);
        categoria.setNombre(nombre);
        return categoria;
    }
}
