package pe.edu.upeu.panaderia.inventario.insumo.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import pe.edu.upeu.panaderia.inventario.insumo.dto.InsumoRequest;
import pe.edu.upeu.panaderia.inventario.insumo.dto.InsumoResponse;
import pe.edu.upeu.panaderia.inventario.insumo.entity.Insumo;

@Mapper(componentModel = "spring")
public interface InsumoMapper {

    InsumoResponse toResponse(Insumo insumo);

    @Mapping(target = "id", ignore = true)
    Insumo toEntity(InsumoRequest request);

    @Mapping(target = "id", ignore = true)
    void actualizar(InsumoRequest request, @MappingTarget Insumo insumo);
}