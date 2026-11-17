package pe.edu.upeu.panaderia.ventas.pago.service;

import java.util.List;
import pe.edu.upeu.panaderia.IdentidadJwt;
import pe.edu.upeu.panaderia.ventas.pago.dto.PagoVentaRequest;
import pe.edu.upeu.panaderia.ventas.pago.dto.PagoVentaResponse;
import pe.edu.upeu.panaderia.ventas.pago.dto.PagoVentaResumenResponse;

public interface PagoVentaService {

    PagoVentaResponse registrar(Long ventaId, PagoVentaRequest request, IdentidadJwt identidad);

    List<PagoVentaResponse> listarPorVenta(Long ventaId, IdentidadJwt identidad);

    PagoVentaResponse obtener(Long pagoId, IdentidadJwt identidad);

    PagoVentaResponse anular(Long pagoId, IdentidadJwt identidad);

    PagoVentaResumenResponse resumen(Long ventaId, IdentidadJwt identidad);

    boolean tienePagosRegistrados(Long ventaId);
}