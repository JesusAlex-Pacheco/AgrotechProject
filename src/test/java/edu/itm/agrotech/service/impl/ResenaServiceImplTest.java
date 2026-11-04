package edu.itm.agrotech.service.impl;

import edu.itm.agrotech.domain.Producto;
import edu.itm.agrotech.dto.ResenaRequest;
import edu.itm.agrotech.entity.Cliente;
import edu.itm.agrotech.entity.Resena;
import edu.itm.agrotech.exception.RecursoNoEncontradoException;
import edu.itm.agrotech.exception.ReglaNegocioException;
import edu.itm.agrotech.repository.ClienteRepository;
import edu.itm.agrotech.repository.PedidoRepository;
import edu.itm.agrotech.repository.ProductoRepository;
import edu.itm.agrotech.repository.ResenaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResenaServiceImplTest {

    @Mock
    private ResenaRepository resenaRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private PedidoRepository pedidoRepository;

    private ResenaServiceImpl servicio;

    @BeforeEach
    void preparar() {
        servicio = new ResenaServiceImpl(resenaRepository, clienteRepository, productoRepository, pedidoRepository);
    }

    @Nested
    @DisplayName("Registrar")
    class Registrar {

        @Test
        @DisplayName("Guarda la resena y la asocia al cliente en los dos lados de la relacion")
        void registra() {
            Cliente cliente = cliente();
            when(productoRepository.buscarPorId(1L)).thenReturn(Optional.of(producto()));
            when(clienteRepository.buscarConUsuario(1L)).thenReturn(Optional.of(cliente));
            when(pedidoRepository.clienteRecibioProducto(1L, 1L)).thenReturn(true);
            when(resenaRepository.existsByClienteIdAndIdProducto(1L, 1L)).thenReturn(false);
            when(resenaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Resena resena = servicio.registrar(1L, new ResenaRequest(1L, 5, "  Llego fresco  "));

            assertThat(resena.getCalificacion()).isEqualTo(5);
            assertThat(resena.getComentario()).isEqualTo("Llego fresco");
            assertThat(resena.getFecha()).isEqualTo(LocalDate.now());
            assertThat(resena.getCliente()).isSameAs(cliente);
            assertThat(cliente.getResenas()).containsExactly(resena);
        }

        @Test
        @DisplayName("Un comentario en blanco se guarda como nulo")
        void comentarioEnBlanco() {
            when(productoRepository.buscarPorId(1L)).thenReturn(Optional.of(producto()));
            when(clienteRepository.buscarConUsuario(1L)).thenReturn(Optional.of(cliente()));
            when(pedidoRepository.clienteRecibioProducto(1L, 1L)).thenReturn(true);
            when(resenaRepository.existsByClienteIdAndIdProducto(1L, 1L)).thenReturn(false);
            when(resenaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThat(servicio.registrar(1L, new ResenaRequest(1L, 4, "   ")).getComentario()).isNull();
        }

        @Test
        @DisplayName("Un producto inexistente lanza 404")
        void productoInexistente() {
            when(productoRepository.buscarPorId(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.registrar(99L, new ResenaRequest(1L, 5, null)))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessage("No existe el producto con id 99");
        }

        @Test
        @DisplayName("Un cliente inexistente lanza 404")
        void clienteInexistente() {
            when(productoRepository.buscarPorId(1L)).thenReturn(Optional.of(producto()));
            when(clienteRepository.buscarConUsuario(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.registrar(1L, new ResenaRequest(99L, 5, null)))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessage("No existe el cliente con id 99");
        }

        @Test
        @DisplayName("Solo resena quien recibio el producto")
        void noLoRecibio() {
            when(productoRepository.buscarPorId(1L)).thenReturn(Optional.of(producto()));
            when(clienteRepository.buscarConUsuario(1L)).thenReturn(Optional.of(cliente()));
            when(pedidoRepository.clienteRecibioProducto(1L, 1L)).thenReturn(false);

            assertThatThrownBy(() -> servicio.registrar(1L, new ResenaRequest(1L, 5, null)))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("Solo se puede resenar un producto ya recibido");
            verify(resenaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Una sola resena por cliente y producto")
        void yaReseno() {
            when(productoRepository.buscarPorId(1L)).thenReturn(Optional.of(producto()));
            when(clienteRepository.buscarConUsuario(1L)).thenReturn(Optional.of(cliente()));
            when(pedidoRepository.clienteRecibioProducto(1L, 1L)).thenReturn(true);
            when(resenaRepository.existsByClienteIdAndIdProducto(1L, 1L)).thenReturn(true);

            assertThatThrownBy(() -> servicio.registrar(1L, new ResenaRequest(1L, 5, null)))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessage("El cliente 1 ya reseno el producto 'Aguacate Hass'");
            verify(resenaRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Listar por producto")
    class ListarPorProducto {

        @Test
        @DisplayName("Devuelve las resenas del producto")
        void lista() {
            List<Resena> resenas = List.of(Resena.nueva(1L, 5, null, LocalDate.now()));
            when(productoRepository.buscarPorId(1L)).thenReturn(Optional.of(producto()));
            when(resenaRepository.listarPorProducto(1L)).thenReturn(resenas);

            assertThat(servicio.listarPorProducto(1L)).isSameAs(resenas);
        }

        @Test
        @DisplayName("Un producto inexistente lanza 404")
        void productoInexistente() {
            when(productoRepository.buscarPorId(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.listarPorProducto(99L))
                    .isInstanceOf(RecursoNoEncontradoException.class);
            verify(resenaRepository, never()).listarPorProducto(any());
        }
    }

    @Nested
    @DisplayName("Listar por cliente")
    class ListarPorCliente {

        @Test
        @DisplayName("Recorre la relacion uno a muchos del cliente")
        void lista() {
            Cliente cliente = cliente();
            Resena resena = Resena.nueva(1L, 4, null, LocalDate.now());
            cliente.agregarResena(resena);
            when(clienteRepository.buscarConResenas(1L)).thenReturn(Optional.of(cliente));

            assertThat(servicio.listarPorCliente(1L)).containsExactly(resena);
        }

        @Test
        @DisplayName("Un cliente inexistente lanza 404")
        void clienteInexistente() {
            when(clienteRepository.buscarConResenas(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.listarPorCliente(99L))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessage("No existe el cliente con id 99");
        }
    }

    private static Producto producto() {
        Producto producto = new Producto();
        producto.setId(1L);
        producto.setNombre("Aguacate Hass");
        return producto;
    }

    private static Cliente cliente() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        return cliente;
    }
}
