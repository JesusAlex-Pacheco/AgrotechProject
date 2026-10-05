package edu.itm.agrotech.repository;

/**
 * Contrato de consulta de clientes. La capa de negocio lo usa para validar
 * que el cliente exista antes de registrar o consultar sus pedidos.
 */
public interface ClienteRepository {

    boolean existe(Long id);
}
