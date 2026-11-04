package edu.itm.agrotech.service.impl;

import edu.itm.agrotech.domain.Rol;
import edu.itm.agrotech.dto.DatosUsuarioRequest;
import edu.itm.agrotech.dto.RegistroAgricultorRequest;
import edu.itm.agrotech.dto.RegistroClienteRequest;
import edu.itm.agrotech.dto.RegistroTransportadorRequest;
import edu.itm.agrotech.entity.Agricultor;
import edu.itm.agrotech.entity.Cliente;
import edu.itm.agrotech.entity.Transportador;
import edu.itm.agrotech.exception.RecursoNoEncontradoException;
import edu.itm.agrotech.exception.ReglaNegocioException;
import edu.itm.agrotech.repository.AgricultorRepository;
import edu.itm.agrotech.repository.ClienteRepository;
import edu.itm.agrotech.repository.TransportadorRepository;
import edu.itm.agrotech.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AgricultorRepository agricultorRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private TransportadorRepository transportadorRepository;
    @Mock
    private PasswordEncoder codificadorDeClaves;

    private UsuarioServiceImpl servicio;

    @BeforeEach
    void preparar() {
        servicio = new UsuarioServiceImpl(usuarioRepository, agricultorRepository, clienteRepository,
                transportadorRepository, codificadorDeClaves);
    }

    @Nested
    @DisplayName("Registrar agricultor")
    class RegistrarAgricultor {

        @Test
        @DisplayName("Crea el usuario con rol AGRICULTOR, correo normalizado y clave cifrada")
        void registra() {
            when(usuarioRepository.existsByCorreoIgnoreCase("marta@agrotech.co")).thenReturn(false);
            when(codificadorDeClaves.encode("Campo2026*")).thenReturn("hash-bcrypt");
            when(agricultorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Agricultor agricultor = servicio.registrarAgricultor(new RegistroAgricultorRequest(
                    datos(" Marta@AgroTech.co "), " Finca El Roble ", "Rionegro", "Llanogrande"));

            assertThat(agricultor.getUsuario().getRol()).isEqualTo(Rol.AGRICULTOR);
            assertThat(agricultor.getUsuario().getCorreo()).isEqualTo("marta@agrotech.co");
            assertThat(agricultor.getUsuario().getClaveHash()).isEqualTo("hash-bcrypt");
            assertThat(agricultor.getNombreFinca()).isEqualTo("Finca El Roble");
        }

        @Test
        @DisplayName("Rechaza un correo ya registrado")
        void correoRepetido() {
            when(usuarioRepository.existsByCorreoIgnoreCase("marta@agrotech.co")).thenReturn(true);

            assertThatThrownBy(() -> servicio.registrarAgricultor(new RegistroAgricultorRequest(
                    datos("marta@agrotech.co"), "Finca", "Rionegro", null)))
                    .isInstanceOf(ReglaNegocioException.class)
                    .hasMessageContaining("Ya existe un usuario registrado con el correo");
            verify(agricultorRepository, never()).save(any());
            verify(codificadorDeClaves, never()).encode(any());
        }
    }

    @Nested
    @DisplayName("Registrar cliente")
    class RegistrarCliente {

        @Test
        @DisplayName("Crea el usuario con rol CLIENTE y su direccion")
        void registra() {
            when(usuarioRepository.existsByCorreoIgnoreCase("marta@agrotech.co")).thenReturn(false);
            when(codificadorDeClaves.encode("Campo2026*")).thenReturn("hash-bcrypt");
            when(clienteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Cliente cliente = servicio.registrarCliente(
                    new RegistroClienteRequest(datos("marta@agrotech.co"), "Carrera 43A # 1-50"));

            assertThat(cliente.getUsuario().getRol()).isEqualTo(Rol.CLIENTE);
            assertThat(cliente.getDireccion()).isEqualTo("Carrera 43A # 1-50");
        }

        @Test
        @DisplayName("Rechaza un correo ya registrado")
        void correoRepetido() {
            when(usuarioRepository.existsByCorreoIgnoreCase("marta@agrotech.co")).thenReturn(true);

            assertThatThrownBy(() -> servicio.registrarCliente(
                    new RegistroClienteRequest(datos("marta@agrotech.co"), null)))
                    .isInstanceOf(ReglaNegocioException.class);
            verify(clienteRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Registrar transportador")
    class RegistrarTransportador {

        @Test
        @DisplayName("Crea el usuario con rol TRANSPORTADOR y la placa en mayusculas")
        void registra() {
            when(usuarioRepository.existsByCorreoIgnoreCase("marta@agrotech.co")).thenReturn(false);
            when(codificadorDeClaves.encode("Campo2026*")).thenReturn("hash-bcrypt");
            when(transportadorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Transportador transportador = servicio.registrarTransportador(
                    new RegistroTransportadorRequest(datos("marta@agrotech.co"), " abc123 ", "Camioneta"));

            assertThat(transportador.getUsuario().getRol()).isEqualTo(Rol.TRANSPORTADOR);
            assertThat(transportador.getPlaca()).isEqualTo("ABC123");
        }

        @Test
        @DisplayName("Rechaza un correo ya registrado")
        void correoRepetido() {
            when(usuarioRepository.existsByCorreoIgnoreCase("marta@agrotech.co")).thenReturn(true);

            assertThatThrownBy(() -> servicio.registrarTransportador(
                    new RegistroTransportadorRequest(datos("marta@agrotech.co"), "ABC123", null)))
                    .isInstanceOf(ReglaNegocioException.class);
            verify(transportadorRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Consultar")
    class Consultar {

        @Test
        @DisplayName("Agricultor encontrado")
        void agricultor() {
            Agricultor agricultor = new Agricultor();
            when(agricultorRepository.buscarConUsuario(1L)).thenReturn(Optional.of(agricultor));

            assertThat(servicio.consultarAgricultor(1L)).isSameAs(agricultor);
        }

        @Test
        @DisplayName("Agricultor inexistente lanza 404")
        void agricultorInexistente() {
            when(agricultorRepository.buscarConUsuario(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.consultarAgricultor(99L))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessage("No existe el agricultor con id 99");
        }

        @Test
        @DisplayName("Cliente encontrado")
        void cliente() {
            Cliente cliente = new Cliente();
            when(clienteRepository.buscarConUsuario(1L)).thenReturn(Optional.of(cliente));

            assertThat(servicio.consultarCliente(1L)).isSameAs(cliente);
        }

        @Test
        @DisplayName("Cliente inexistente lanza 404")
        void clienteInexistente() {
            when(clienteRepository.buscarConUsuario(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.consultarCliente(99L))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessage("No existe el cliente con id 99");
        }

        @Test
        @DisplayName("Transportador encontrado")
        void transportador() {
            Transportador transportador = new Transportador();
            when(transportadorRepository.buscarConUsuario(1L)).thenReturn(Optional.of(transportador));

            assertThat(servicio.consultarTransportador(1L)).isSameAs(transportador);
        }

        @Test
        @DisplayName("Transportador inexistente lanza 404")
        void transportadorInexistente() {
            when(transportadorRepository.buscarConUsuario(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.consultarTransportador(99L))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessage("No existe el transportador con id 99");
        }
    }

    private static DatosUsuarioRequest datos(String correo) {
        return new DatosUsuarioRequest(" Marta Lucia Ospina ", correo, "Campo2026*", "3107778899");
    }
}
