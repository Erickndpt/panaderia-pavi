package pe.edu.upeu.panaderia.inventario.existencia.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pe.edu.upeu.panaderia.administracion.producto.dto.ProductoResponse;
import pe.edu.upeu.panaderia.inventario.existencia.dto.ExistenciaResponse;
import pe.edu.upeu.panaderia.inventario.existencia.entity.Existencia;

@Mapper(componentModel = "spring")
public interface ExistenciaMapper {

    @Mapping(target = "id", source = "existencia.id")
    @Mapping(target = "cantidad", source = "existencia.cantidad")
    @Mapping(target = "producto.id", source = "producto.id")
    @Mapping(target = "producto.nombre", source = "producto.nombre")
    ExistenciaResponse toResponse(Existencia existencia, ProductoResponse producto);
}
