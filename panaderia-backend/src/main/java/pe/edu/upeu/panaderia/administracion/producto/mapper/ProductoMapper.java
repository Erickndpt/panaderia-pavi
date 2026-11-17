package pe.edu.upeu.panaderia.administracion.producto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import pe.edu.upeu.panaderia.administracion.categoria.entity.Categoria;
import pe.edu.upeu.panaderia.administracion.categoria.mapper.CategoriaMapper;
import pe.edu.upeu.panaderia.administracion.producto.dto.ProductoRequest;
import pe.edu.upeu.panaderia.administracion.producto.dto.ProductoResponse;
import pe.edu.upeu.panaderia.administracion.producto.entity.Producto;

@Mapper(componentModel = "spring", uses = CategoriaMapper.class)
public interface ProductoMapper {

    ProductoResponse toResponse(Producto producto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "nombre", source = "request.nombre")
    @Mapping(target = "categoria", source = "categoria")
    Producto toEntity(ProductoRequest request, Categoria categoria);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "nombre", source = "request.nombre")
    @Mapping(target = "categoria", source = "categoria")
    void actualizar(ProductoRequest request, @MappingTarget Producto producto, Categoria categoria);
}