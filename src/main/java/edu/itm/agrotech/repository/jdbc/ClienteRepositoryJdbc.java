package edu.itm.agrotech.repository.jdbc;

import edu.itm.agrotech.repository.ClienteRepository;
import org.springframework.stereotype.Repository;

@Repository
public class ClienteRepositoryJdbc implements ClienteRepository {

    private static final String SQL_EXISTE = """
            SELECT 1
            FROM cliente
            WHERE id_cliente = ?
            """;

    private final EjecutorJdbc jdbc;

    public ClienteRepositoryJdbc(EjecutorJdbc jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean existe(Long id) {
        return jdbc.consultarUno(SQL_EXISTE,
                sentencia -> sentencia.setLong(1, id),
                fila -> fila.getInt(1),
                "No fue posible consultar el cliente").isPresent();
    }
}
