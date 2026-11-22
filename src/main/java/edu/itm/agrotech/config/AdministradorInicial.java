package edu.itm.agrotech.config;

import edu.itm.agrotech.domain.Rol;
import edu.itm.agrotech.entity.Usuario;
import edu.itm.agrotech.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Crea la cuenta de administrador al arrancar, si aun no existe.
 *
 * <p>El registro publico (POST /api/clientes, /agricultores, /transportadores)
 * nunca asigna el rol ADMIN, de modo que esta es la unica forma de tener un
 * administrador: solo quien controla la configuracion del backend puede
 * crearlo. Se configura con agrotech.admin.* en application.properties, que
 * guarda el hash BCrypt de la clave y nunca la clave en texto plano.
 */
@Component
public class AdministradorInicial implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(AdministradorInicial.class);

    private final UsuarioRepository usuarioRepository;
    private final String nombre;
    private final String correo;
    private final String claveHash;

    public AdministradorInicial(UsuarioRepository usuarioRepository,
                                @Value("${agrotech.admin.nombre:Administrador}") String nombre,
                                @Value("${agrotech.admin.correo:}") String correo,
                                @Value("${agrotech.admin.clave-hash:}") String claveHash) {
        this.usuarioRepository = usuarioRepository;
        this.nombre = nombre;
        this.correo = correo.trim();
        this.claveHash = claveHash.trim();
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        crearSiNoExiste();
    }

    void crearSiNoExiste() {
        if (correo.isEmpty() || claveHash.isEmpty()) {
            LOG.info("No se configuro una cuenta de administrador (agrotech.admin.*)");
            return;
        }
        // Un hash BCrypt empieza por $2a$, $2b$ o $2y$
        if (!claveHash.startsWith("$2")) {
            LOG.warn("agrotech.admin.clave-hash no es un hash BCrypt: no se crea el administrador");
            return;
        }

        usuarioRepository.findByCorreoIgnoreCase(correo).ifPresentOrElse(
                existente -> {
                    if (existente.getRol() != Rol.ADMIN) {
                        LOG.warn("El correo {} ya pertenece a un usuario con rol {}: no se crea el administrador",
                                correo, existente.getRol());
                    }
                },
                () -> {
                    Usuario admin = new Usuario();
                    admin.setNombre(nombre);
                    admin.setCorreo(correo);
                    admin.setClaveHash(claveHash);
                    admin.setRol(Rol.ADMIN);
                    usuarioRepository.save(admin);
                    LOG.info("Cuenta de administrador creada: {}", correo);
                });
    }
}
