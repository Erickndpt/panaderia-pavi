package pe.edu.upeu.panaderia.administracion.producto.service;

import java.math.BigDecimal;
import java.util.List;
import pe.edu.upeu.panaderia.administracion.producto.dto.ProductoRequest;
import pe.edu.upeu.panaderia.administracion.producto.dto.ProductoResponse;

public interface ProductoService {

    List<ProductoResponse> listar(String nombre, Long categoriaId, BigDecimal precioMin, BigDecimal precioMax);

    ProductoResponse obtener(Long id);

    ProductoResponse crear(ProductoRequest request);

    ProductoResponse actualizar(Long id, ProductoRequest request);

    void eliminar(Long id);
}