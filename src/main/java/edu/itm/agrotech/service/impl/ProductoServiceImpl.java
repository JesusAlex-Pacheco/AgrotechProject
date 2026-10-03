package edu.itm.agrotech.service.impl;

import edu.itm.agrotech.domain.Producto;
import edu.itm.agrotech.dto.ProductoRequest;
import edu.itm.agrotech.exception.RecursoNoEncontradoException;
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

    public ProductoServiceImpl(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    @Override
    public Producto crear(ProductoRequest solicitud) {
        Producto producto = new Producto();
        producto.setIdAgricultor(solicitud.idAgricultor());
        producto.setIdCategoria(solicitud.idCategoria());
        producto.setNombre(solicitud.nombre().trim());
        producto.setDescripcion(solicitud.descripcion());
        producto.setPrecio(solicitud.precio());
        producto.setUnidadMedida(solicitud.unidadMedida());
        producto.setStock(solicitud.stock());
        producto.setFotoUrl(solicitud.fotoUrl());

        // Regla: un producto nace disponible solo si tiene existencias.
        producto.setDisponible(solicitud.stock().compareTo(BigDecimal.ZERO) > 0);

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

        producto.setIdCategoria(solicitud.idCategoria());
        producto.setNombre(solicitud.nombre().trim());
        producto.setDescripcion(solicitud.descripcion());
        producto.setPrecio(solicitud.precio());
        producto.setUnidadMedida(solicitud.unidadMedida());
        producto.setStock(solicitud.stock());
        producto.setFotoUrl(solicitud.fotoUrl());
        producto.setDisponible(solicitud.stock().compareTo(BigDecimal.ZERO) > 0);

        productoRepository.actualizar(producto);
        return producto;
    }

    @Override
    public void desactivar(Long id) {
        consultar(id);
        productoRepository.desactivar(id);
    }
}
