package edu.itm.agrotech.service.impl;

import edu.itm.agrotech.domain.Categoria;
import edu.itm.agrotech.dto.CategoriaRequest;
import edu.itm.agrotech.exception.ReglaNegocioException;
import edu.itm.agrotech.repository.CategoriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @Test
    @DisplayName("Crear: rechaza un nombre que ya existe")
    void crearNombreRepetido() {
        when(categoriaRepository.buscarPorNombre("frutas")).thenReturn(Optional.of(categoria(1L, "Frutas")));

        assertThatThrownBy(() -> servicio.crear(new CategoriaRequest(" frutas ")))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessage("Ya existe la categoria 'Frutas'");
        verify(categoriaRepository, never()).guardar(any());
    }

    @Test
    @DisplayName("Actualizar: puede conservar su propio nombre")
    void actualizarConSuMismoNombre() {
        Categoria frutas = categoria(1L, "Frutas");
        when(categoriaRepository.buscarPorId(1L)).thenReturn(Optional.of(frutas));
        when(categoriaRepository.buscarPorNombre("Frutas")).thenReturn(Optional.of(frutas));

        Categoria actualizada = servicio.actualizar(1L, new CategoriaRequest("Frutas"));

        assertThat(actualizada.getNombre()).isEqualTo("Frutas");
        verify(categoriaRepository).actualizar(frutas);
    }

    @Test
    @DisplayName("Eliminar: no se permite si tiene productos")
    void eliminarConProductos() {
        when(categoriaRepository.buscarPorId(1L)).thenReturn(Optional.of(categoria(1L, "Frutas")));
        when(categoriaRepository.tieneProductos(1L)).thenReturn(true);

        assertThatThrownBy(() -> servicio.eliminar(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("tiene productos asociados");
        verify(categoriaRepository, never()).eliminar(any());
    }

    @Test
    @DisplayName("Eliminar: sin productos se elimina")
    void eliminarSinProductos() {
        when(categoriaRepository.buscarPorId(5L)).thenReturn(Optional.of(categoria(5L, "Granos")));
        when(categoriaRepository.tieneProductos(5L)).thenReturn(false);

        servicio.eliminar(5L);

        verify(categoriaRepository).eliminar(5L);
    }

    private static Categoria categoria(Long id, String nombre) {
        Categoria categoria = new Categoria();
        categoria.setId(id);
        categoria.setNombre(nombre);
        return categoria;
    }
}
