package pe.edu.upeu.panaderia.compras.compra.mapper;

import org.mapstruct.Mapper;
import pe.edu.upeu.panaderia.compras.compra.dto.CompraResponse;
import pe.edu.upeu.panaderia.compras.compra.dto.DetalleCompraResponse;
import pe.edu.upeu.panaderia.compras.compra.entity.Compra;
import pe.edu.upeu.panaderia.compras.compra.entity.DetalleCompra;
import pe.edu.upeu.panaderia.compras.proveedor.dto.ProveedorResumen;
import pe.edu.upeu.panaderia.compras.proveedor.entity.Proveedor;

@Mapper(componentModel = "spring")
public interface CompraMapper {

    CompraResponse toResponse(Compra compra);

    DetalleCompraResponse toDetalleResponse(DetalleCompra detalle);

    ProveedorResumen toProveedorResumen(Proveedor proveedor);
}