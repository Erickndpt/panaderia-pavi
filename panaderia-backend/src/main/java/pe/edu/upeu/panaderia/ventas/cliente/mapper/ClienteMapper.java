package pe.edu.upeu.panaderia.ventas.cliente.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import pe.edu.upeu.panaderia.ventas.cliente.dto.ClienteRequest;
import pe.edu.upeu.panaderia.ventas.cliente.dto.ClienteResponse;
import pe.edu.upeu.panaderia.ventas.cliente.entity.Cliente;

@Mapper(componentModel = "spring")
public interface ClienteMapper {

    ClienteResponse toResponse(Cliente cliente);

    @Mapping(target = "id", ignore = true)
    Cliente toEntity(ClienteRequest request);

    @Mapping(target = "id", ignore = true)
    void actualizar(ClienteRequest request, @MappingTarget Cliente cliente);
}
