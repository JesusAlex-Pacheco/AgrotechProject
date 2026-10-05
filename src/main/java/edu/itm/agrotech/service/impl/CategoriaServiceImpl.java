package edu.itm.agrotech.service.impl;

import edu.itm.agrotech.domain.Categoria;
import edu.itm.agrotech.dto.CategoriaRequest;
import edu.itm.agrotech.exception.RecursoNoEncontradoException;
import edu.itm.agrotech.exception.ReglaNegocioException;
import edu.itm.agrotech.repository.CategoriaRepository;
import edu.itm.agrotech.service.CategoriaService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Capa de negocio de Categoria. Aplica las reglas antes de delegar la
 * persistencia al repositorio. No conoce HTTP ni SQL.
 */
@Service
public class CategoriaServiceImpl implements CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaServiceImpl(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @Override
    public Categoria crear(CategoriaRequest solicitud) {
        String nombre = solicitud.nombre().trim();
        validarNombreUnico(nombre, null);

        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        return categoriaRepository.guardar(categoria);
    }

    @Override
    public Categoria consultar(Long id) {
        return categoriaRepository.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la categoria con id " + id));
    }

    @Override
    public List<Categoria> listar() {
        return categoriaRepository.listar();
    }

    @Override
    public Categoria actualizar(Long id, CategoriaRequest solicitud) {
        Categoria categoria = consultar(id);
        String nombre = solicitud.nombre().trim();
        validarNombreUnico(nombre, id);

        categoria.setNombre(nombre);
        categoriaRepository.actualizar(categoria);
        return categoria;
    }

    @Override
    public void eliminar(Long id) {
        Categoria categoria = consultar(id);

        if (categoriaRepository.tieneProductos(id)) {
            throw new ReglaNegocioException(
                    "No se puede eliminar la categoria '" + categoria.getNombre()
                            + "' porque tiene productos asociados");
        }

        categoriaRepository.eliminar(id);
    }

    /** Regla: no puede haber dos categorias con el mismo nombre. */
    private void validarNombreUnico(String nombre, Long idActual) {
        categoriaRepository.buscarPorNombre(nombre)
                .filter(existente -> !existente.getId().equals(idActual))
                .ifPresent(existente -> {
                    throw new ReglaNegocioException(
                            "Ya existe la categoria '" + existente.getNombre() + "'");
                });
    }
}
