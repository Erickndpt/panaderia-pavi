package pe.edu.upeu.panaderia.finanzas.movimiento.mapper;

import org.mapstruct.Mapper;
import pe.edu.upeu.panaderia.finanzas.movimiento.dto.MovimientoFinancieroResponse;
import pe.edu.upeu.panaderia.finanzas.movimiento.entity.MovimientoFinanciero;

@Mapper(componentModel = "spring")
public interface MovimientoFinancieroMapper {

    MovimientoFinancieroResponse toResponse(MovimientoFinanciero movimiento);
}