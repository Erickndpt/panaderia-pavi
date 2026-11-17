package pe.edu.upeu.panaderia.ventas.venta.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pe.edu.upeu.panaderia.ventas.cliente.dto.ClienteResumen;
import pe.edu.upeu.panaderia.ventas.cliente.entity.Cliente;
import pe.edu.upeu.panaderia.ventas.venta.dto.DetalleVentaResponse;
import pe.edu.upeu.panaderia.ventas.venta.dto.VendedorResumen;
import pe.edu.upeu.panaderia.ventas.venta.dto.VentaResponse;
import pe.edu.upeu.panaderia.ventas.venta.entity.DetalleVenta;
import pe.edu.upeu.panaderia.ventas.venta.entity.Venta;

@Mapper(componentModel = "spring")
public interface VentaMapper {

    @Mapping(target = "vendedor", source = ".")
    VentaResponse toResponse(Venta venta);

    @Mapping(target = "productoId", source = "idProducto")
    DetalleVentaResponse toResponse(DetalleVenta detalle);

    ClienteResumen toClienteResumen(Cliente cliente);

    default VendedorResumen toVendedorResumen(Venta venta) {
        if (venta == null) {
            return null;
        }
        boolean sinUsername = venta.getVendedorUsername() == null || venta.getVendedorUsername().isBlank();
        boolean sinNombre = venta.getVendedorNombre() == null || venta.getVendedorNombre().isBlank();
        if (sinUsername && sinNombre) {
            return null;
        }
        return new VendedorResumen(
                venta.getVendedorId(),
                venta.getVendedorUsername(),
                venta.getVendedorNombre());
    }
}
