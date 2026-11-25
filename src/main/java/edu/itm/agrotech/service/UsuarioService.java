package edu.itm.agrotech.service;

import edu.itm.agrotech.domain.SesionUsuario;
import edu.itm.agrotech.dto.RegistroAgricultorRequest;
import edu.itm.agrotech.dto.RegistroClienteRequest;
import edu.itm.agrotech.dto.RegistroTransportadorRequest;
import edu.itm.agrotech.entity.Agricultor;
import edu.itm.agrotech.entity.Cliente;
import edu.itm.agrotech.entity.Transportador;

import java.util.List;

/**
 * Contrato de negocio del registro de usuarios por rol. Cada registro crea
 * el usuario (datos de acceso) y su perfil en una sola transaccion.
 */
public interface UsuarioService {

    /** @throws edu.itm.agrotech.exception.ReglaNegocioException si el correo ya esta registrado */
    Agricultor registrarAgricultor(RegistroAgricultorRequest solicitud);

    /** @throws edu.itm.agrotech.exception.RecursoNoEncontradoException si no existe */
    Agricultor consultarAgricultor(Long id);

    /** @throws edu.itm.agrotech.exception.ReglaNegocioException si el correo ya esta registrado */
    Cliente registrarCliente(RegistroClienteRequest solicitud);

    /** @throws edu.itm.agrotech.exception.RecursoNoEncontradoException si no existe */
    Cliente consultarCliente(Long id);

    /** @throws edu.itm.agrotech.exception.ReglaNegocioException si el correo ya esta registrado */
    Transportador registrarTransportador(RegistroTransportadorRequest solicitud);

    /** @throws edu.itm.agrotech.exception.RecursoNoEncontradoException si no existe */
    Transportador consultarTransportador(Long id);

    /** Todos los transportadores con su usuario, por nombre, para asignarlos a una entrega. */
    List<Transportador> listarTransportadores();

    /**
     * Verifica el correo y la clave y devuelve el usuario con el id de su perfil.
     *
     * @throws edu.itm.agrotech.exception.CredencialesInvalidasException si el correo
     *         no existe o la clave no coincide
     */
    SesionUsuario iniciarSesion(String correo, String clave);
}
