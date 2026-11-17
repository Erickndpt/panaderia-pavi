package pe.edu.upeu.panaderia.produccion.orden.mapper;

import org.mapstruct.Mapper;
import pe.edu.upeu.panaderia.produccion.orden.dto.DetalleConsumoResponse;
import pe.edu.upeu.panaderia.produccion.orden.dto.OrdenProduccionResponse;
import pe.edu.upeu.panaderia.produccion.orden.entity.DetalleConsumo;
import pe.edu.upeu.panaderia.produccion.orden.entity.OrdenProduccion;

@Mapper(componentModel = "spring")
public interface OrdenProduccionMapper {

    OrdenProduccionResponse toResponse(OrdenProduccion orden);

    DetalleConsumoResponse toDetalleResponse(DetalleConsumo detalle);
}