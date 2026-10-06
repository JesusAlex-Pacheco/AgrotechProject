package edu.itm.agrotech.service.impl;

import edu.itm.agrotech.domain.Producto;
import edu.itm.agrotech.dto.ProductoRequest;
import edu.itm.agrotech.exception.RecursoNoEncontradoException;
import edu.itm.agrotech.exception.ReglaNegocioException;
import edu.itm.agrotech.repository.AgricultorRepository;
import edu.itm.agrotech.repository.CategoriaRepository;
import edu.itm.agrotech.repository.ProductoRepository;
import edu.itm.agrotech.service.ProductoService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Capa de negocio. Aplica las reglas antes de delegar la persistencia
 * al repositorio. No conoce HTTP ni SQL.
 */
@Service
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final AgricultorRepository agricultorRepository;

    public ProductoServiceImpl(ProductoRepository productoRepository,
                               CategoriaRepository categoriaRepository,
                               AgricultorRepository agricultorRepository) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.agricultorRepository = agricultorRepository;
    }

    @Override
    public Producto crear(ProductoRequest solicitud) {
        validarAgricultorExiste(solicitud.idAgricultor());
        validarCategoriaExiste(solicitud.idCategoria());

        Producto producto = new Producto();
        producto.setIdAgricultor(solicitud.idAgricultor());
        aplicarDatos(producto, solicitud);

        return productoRepository.guardar(producto);
    }

    @Override
    public Producto consultar(Long id) {
        return productoRepository.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el producto con id " + id));
    }

    @Override
    public List<Producto> listar(Long idCategoria, String nombre) {
        return productoRepository.listar(idCategoria, nombre);
    }

    @Override
    public Producto actualizar(Long id, ProductoRequest solicitud) {
        Producto producto = consultar(id);

        // Regla: un producto pertenece siempre al agricultor que lo publico.
        if (!producto.getIdAgricultor().equals(solicitud.idAgricultor())) {
            throw new ReglaNegocioException("Un producto no puede cambiar de agricultor: "
                    + "pertenece al agricultor " + producto.getIdAgricultor());
        }
        validarCategoriaExiste(solicitud.idCategoria());

        aplicarDatos(producto, solicitud);
        productoRepository.actualizar(producto);
        return producto;
    }

    @Override
    public void desactivar(Long id) {
        consultar(id);
        productoRepository.desactivar(id);
    }

    /** Datos editables del producto, comunes a la creacion y la actualizacion. */
    private void aplicarDatos(Producto producto, ProductoRequest solicitud) {
        producto.setIdCategoria(solicitud.idCategoria());
        producto.setNombre(solicitud.nombre().trim());
        producto.setDescripcion(solicitud.descripcion());
        producto.setPrecio(solicitud.precio());
        producto.setUnidadMedida(solicitud.unidadMedida());
        producto.setStock(solicitud.stock());
        producto.setFotoUrl(solicitud.fotoUrl());

        // Regla: un producto solo esta disponible si tiene existencias.
        producto.setDisponible(solicitud.stock().compareTo(BigDecimal.ZERO) > 0);
    }

    private void validarAgricultorExiste(Long idAgricultor) {
        if (!agricultorRepository.existe(idAgricultor)) {
            throw new RecursoNoEncontradoException(
                    "No existe el agricultor con id " + idAgricultor);
        }
    }

    private void validarCategoriaExiste(Long idCategoria) {
        if (categoriaRepository.buscarPorId(idCategoria).isEmpty()) {
            throw new RecursoNoEncontradoException(
                    "No existe la categoria con id " + idCategoria);
        }
    }
}
