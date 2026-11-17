package pe.edu.upeu.panaderia.administracion.categoria.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import pe.edu.upeu.panaderia.administracion.categoria.dto.CategoriaRequest;
import pe.edu.upeu.panaderia.administracion.categoria.dto.CategoriaResumen;
import pe.edu.upeu.panaderia.administracion.categoria.dto.CategoriaResponse;
import pe.edu.upeu.panaderia.administracion.categoria.entity.Categoria;

@Mapper(componentModel = "spring")
public interface CategoriaMapper {

    CategoriaResponse toResponse(Categoria categoria);

    CategoriaResumen toResumen(Categoria categoria);

    @Mapping(target = "id", ignore = true)
    Categoria toEntity(CategoriaRequest request);

    @Mapping(target = "id", ignore = true)
    void actualizar(CategoriaRequest request, @MappingTarget Categoria categoria);
}