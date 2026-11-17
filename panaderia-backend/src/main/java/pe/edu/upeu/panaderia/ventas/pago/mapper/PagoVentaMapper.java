package pe.edu.upeu.panaderia.ventas.pago.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pe.edu.upeu.panaderia.ventas.pago.dto.PagoVentaResponse;
import pe.edu.upeu.panaderia.ventas.pago.entity.PagoVenta;

@Mapper(componentModel = "spring")
public interface PagoVentaMapper {

    @Mapping(target = "ventaId", source = "venta.id")
    PagoVentaResponse toResponse(PagoVenta pago);
}