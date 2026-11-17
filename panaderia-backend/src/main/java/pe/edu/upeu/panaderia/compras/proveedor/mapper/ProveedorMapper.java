package pe.edu.upeu.panaderia.compras.proveedor.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import pe.edu.upeu.panaderia.compras.proveedor.dto.ProveedorRequest;
import pe.edu.upeu.panaderia.compras.proveedor.dto.ProveedorResponse;
import pe.edu.upeu.panaderia.compras.proveedor.entity.Proveedor;

@Mapper(componentModel = "spring")
public interface ProveedorMapper {

    ProveedorResponse toResponse(Proveedor proveedor);

    @Mapping(target = "id", ignore = true)
    Proveedor toEntity(ProveedorRequest request);

    @Mapping(target = "id", ignore = true)
    void actualizar(ProveedorRequest request, @MappingTarget Proveedor proveedor);
}