package edu.itm.agrotech.config;

import edu.itm.agrotech.domain.Rol;
import edu.itm.agrotech.entity.Usuario;
import edu.itm.agrotech.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdministradorInicialTest {

    private static final String HASH = "$2a$10$abcdefghijklmnopqrstuuJ1ZqfWm0Y7uQ0w8G3m5mF1pQyq1s2a";

    @Mock
    private UsuarioRepository usuarioRepository;

    private AdministradorInicial inicial(String correo, String hash) {
        return new AdministradorInicial(usuarioRepository, "Administrador AgroTech", correo, hash);
    }

    @Test
    @DisplayName("Crea el administrador con rol ADMIN y el hash configurado")
    void creaSiNoExiste() {
        when(usuarioRepository.findByCorreoIgnoreCase("admin@agrotech.co")).thenReturn(Optional.empty());

        inicial("admin@agrotech.co", HASH).crearSiNoExiste();

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(guardado.capture());
        assertThat(guardado.getValue().getRol()).isEqualTo(Rol.ADMIN);
        assertThat(guardado.getValue().getClaveHash()).isEqualTo(HASH);
        assertThat(guardado.getValue().getCorreo()).isEqualTo("admin@agrotech.co");
    }

    @Test
    @DisplayName("No lo duplica si ya existe")
    void yaExiste() {
        Usuario admin = new Usuario();
        admin.setRol(Rol.ADMIN);
        when(usuarioRepository.findByCorreoIgnoreCase("admin@agrotech.co")).thenReturn(Optional.of(admin));

        inicial("admin@agrotech.co", HASH).crearSiNoExiste();

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("No convierte en administrador a un usuario que ya usa ese correo")
    void correoDeOtroRol() {
        Usuario cliente = new Usuario();
        cliente.setRol(Rol.CLIENTE);
        when(usuarioRepository.findByCorreoIgnoreCase("admin@agrotech.co")).thenReturn(Optional.of(cliente));

        inicial("admin@agrotech.co", HASH).crearSiNoExiste();

        verify(usuarioRepository, never()).save(any());
        assertThat(cliente.getRol()).isEqualTo(Rol.CLIENTE);
    }

    @Test
    @DisplayName("Sin configuracion, o con una clave en texto plano en vez de un hash, no hace nada")
    void sinConfiguracion() {
        inicial("", HASH).crearSiNoExiste();
        inicial("admin@agrotech.co", "").crearSiNoExiste();
        inicial("admin@agrotech.co", "MiClave123").crearSiNoExiste();

        verifyNoInteractions(usuarioRepository);
    }
}
