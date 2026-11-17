package pe.edu.upeu.panaderia.administracion.producto.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.panaderia.administracion.categoria.entity.Categoria;
import pe.edu.upeu.panaderia.administracion.categoria.repository.CategoriaRepository;
import pe.edu.upeu.panaderia.administracion.producto.dto.ProductoRequest;
import pe.edu.upeu.panaderia.administracion.producto.dto.ProductoResponse;
import pe.edu.upeu.panaderia.administracion.producto.entity.Producto;
import pe.edu.upeu.panaderia.administracion.producto.mapper.ProductoMapper;
import pe.edu.upeu.panaderia.administracion.producto.repository.ProductoRepository;
import pe.edu.upeu.panaderia.administracion.producto.specification.ProductoSpecifications;
import pe.edu.upeu.panaderia.exception.FiltroInvalidoException;
import pe.edu.upeu.panaderia.exception.ResourceNotFoundException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProductoMapper productoMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> listar(String nombre, Long categoriaId, BigDecimal precioMin, BigDecimal precioMax) {
        validarRangoPrecios(precioMin, precioMax);
        if (categoriaId != null) {
            buscarCategoriaOFallar(categoriaId);
        }
        Specification<Producto> filtros = construirFiltros(nombre, categoriaId, precioMin, precioMax);
        List<Producto> productos = filtros == null
                ? productoRepository.findAll()
                : productoRepository.findAll(filtros);
        return productos.stream()
                .map(productoMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id) {
        return productoMapper.toResponse(buscarOFallar(id));
    }

    @Override
    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        Categoria categoria = buscarCategoriaOFallar(request.getCategoriaId());
        Producto producto = productoRepository.save(productoMapper.toEntity(request, categoria));
        log.info("Producto creado con id {}", producto.getId());
        return productoMapper.toResponse(producto);
    }

    @Override
    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto producto = buscarOFallar(id);
        Categoria categoria = buscarCategoriaOFallar(request.getCategoriaId());
        productoMapper.actualizar(request, producto, categoria);
        log.info("Producto actualizado con id {}", id);
        return productoMapper.toResponse(producto);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Producto producto = buscarOFallar(id);
        productoRepository.delete(producto);
        log.info("Producto eliminado con id {}", id);
    }

    private Producto buscarOFallar(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", id));
    }

    private Categoria buscarCategoriaOFallar(Long categoriaId) {
        return categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría", categoriaId));
    }

    private Specification<Producto> construirFiltros(String nombre, Long categoriaId, BigDecimal precioMin,
                                                     BigDecimal precioMax) {
        List<Specification<Producto>> predicados = new ArrayList<>();
        if (nombre != null && !nombre.isBlank()) {
            predicados.add(ProductoSpecifications.nombreContiene(nombre));
        }
        if (categoriaId != null) {
            predicados.add(ProductoSpecifications.categoriaIgual(categoriaId));
        }
        if (precioMin != null) {
            predicados.add(ProductoSpecifications.precioMayorIgual(precioMin));
        }
        if (precioMax != null) {
            predicados.add(ProductoSpecifications.precioMenorIgual(precioMax));
        }
        return predicados.isEmpty() ? null : Specification.allOf(predicados);
    }

    private void validarRangoPrecios(BigDecimal precioMin, BigDecimal precioMax) {
        if (precioMin != null && precioMax != null && precioMin.compareTo(precioMax) > 0) {
            throw new FiltroInvalidoException("precioMin no debe ser mayor que precioMax");
        }
    }
}