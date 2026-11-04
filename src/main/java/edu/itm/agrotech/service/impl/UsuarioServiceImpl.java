package edu.itm.agrotech.service.impl;

import edu.itm.agrotech.domain.Rol;
import edu.itm.agrotech.dto.DatosUsuarioRequest;
import edu.itm.agrotech.dto.RegistroAgricultorRequest;
import edu.itm.agrotech.dto.RegistroClienteRequest;
import edu.itm.agrotech.dto.RegistroTransportadorRequest;
import edu.itm.agrotech.entity.Agricultor;
import edu.itm.agrotech.entity.Cliente;
import edu.itm.agrotech.entity.Transportador;
import edu.itm.agrotech.entity.Usuario;
import edu.itm.agrotech.exception.RecursoNoEncontradoException;
import edu.itm.agrotech.exception.ReglaNegocioException;
import edu.itm.agrotech.repository.AgricultorRepository;
import edu.itm.agrotech.repository.ClienteRepository;
import edu.itm.agrotech.repository.TransportadorRepository;
import edu.itm.agrotech.repository.UsuarioRepository;
import edu.itm.agrotech.service.UsuarioService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Registro de usuarios por rol con JPA. El usuario se guarda en cascada con
 * su perfil (relacion @OneToOne), asi que ambos quedan en la misma
 * transaccion: si falla el perfil, tampoco se crea el usuario.
 */
@Service
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final AgricultorRepository agricultorRepository;
    private final ClienteRepository clienteRepository;
    private final TransportadorRepository transportadorRepository;
    private final PasswordEncoder codificadorDeClaves;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository,
                              AgricultorRepository agricultorRepository,
                              ClienteRepository clienteRepository,
                              TransportadorRepository transportadorRepository,
                              PasswordEncoder codificadorDeClaves) {
        this.usuarioRepository = usuarioRepository;
        this.agricultorRepository = agricultorRepository;
        this.clienteRepository = clienteRepository;
        this.transportadorRepository = transportadorRepository;
        this.codificadorDeClaves = codificadorDeClaves;
    }

    @Override
    @Transactional
    public Agricultor registrarAgricultor(RegistroAgricultorRequest solicitud) {
        Agricultor agricultor = new Agricultor();
        agricultor.setUsuario(nuevoUsuario(solicitud.usuario(), Rol.AGRICULTOR));
        agricultor.setNombreFinca(solicitud.nombreFinca().trim());
        agricultor.setMunicipio(solicitud.municipio().trim());
        agricultor.setVereda(solicitud.vereda());
        return agricultorRepository.save(agricultor);
    }

    @Override
    @Transactional(readOnly = true)
    public Agricultor consultarAgricultor(Long id) {
        return agricultorRepository.buscarConUsuario(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el agricultor con id " + id));
    }

    @Override
    @Transactional
    public Cliente registrarCliente(RegistroClienteRequest solicitud) {
        Cliente cliente = new Cliente();
        cliente.setUsuario(nuevoUsuario(solicitud.usuario(), Rol.CLIENTE));
        cliente.setDireccion(solicitud.direccion());
        return clienteRepository.save(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public Cliente consultarCliente(Long id) {
        return clienteRepository.buscarConUsuario(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el cliente con id " + id));
    }

    @Override
    @Transactional
    public Transportador registrarTransportador(RegistroTransportadorRequest solicitud) {
        Transportador transportador = new Transportador();
        transportador.setUsuario(nuevoUsuario(solicitud.usuario(), Rol.TRANSPORTADOR));
        transportador.setPlaca(solicitud.placa().trim().toUpperCase(Locale.ROOT));
        transportador.setTipoVehiculo(solicitud.tipoVehiculo());
        return transportadorRepository.save(transportador);
    }

    @Override
    @Transactional(readOnly = true)
    public Transportador consultarTransportador(Long id) {
        return transportadorRepository.buscarConUsuario(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el transportador con id " + id));
    }

    /** Datos de acceso comunes a los tres registros: correo unico y clave cifrada. */
    private Usuario nuevoUsuario(DatosUsuarioRequest datos, Rol rol) {
        String correo = datos.correo().trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new ReglaNegocioException("Ya existe un usuario registrado con el correo " + correo);
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(datos.nombre().trim());
        usuario.setCorreo(correo);
        usuario.setClaveHash(codificadorDeClaves.encode(datos.clave()));
        usuario.setTelefono(datos.telefono());
        usuario.setRol(rol);
        return usuario;
    }
}
