package edu.itm.agrotech.repository;

/**
 * Contrato de consulta de agricultores. Por ahora la capa de negocio solo
 * necesita saber si un agricultor existe antes de asociarle un producto,
 * asi que la interfaz expone unicamente eso.
 */
public interface AgricultorRepository {

    boolean existe(Long id);
}
