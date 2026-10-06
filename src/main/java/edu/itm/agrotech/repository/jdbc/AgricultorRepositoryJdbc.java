package edu.itm.agrotech.repository.jdbc;

import edu.itm.agrotech.repository.AgricultorRepository;
import org.springframework.stereotype.Repository;

@Repository
public class AgricultorRepositoryJdbc implements AgricultorRepository {

    private static final String SQL_EXISTE = """
            SELECT 1
            FROM agricultor
            WHERE id_agricultor = ?
            """;

    private final EjecutorJdbc jdbc;

    public AgricultorRepositoryJdbc(EjecutorJdbc jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean existe(Long id) {
        return jdbc.consultarUno(SQL_EXISTE,
                sentencia -> sentencia.setLong(1, id),
                fila -> fila.getInt(1),
                "No fue posible consultar el agricultor").isPresent();
    }
}
