package pe.edu.upeu.panaderia.ventas.venta.service;

import java.time.LocalDateTime;
import java.util.List;
import pe.edu.upeu.panaderia.IdentidadJwt;
import pe.edu.upeu.panaderia.ventas.venta.dto.VentaRequest;
import pe.edu.upeu.panaderia.ventas.venta.dto.VentaResponse;
import pe.edu.upeu.panaderia.ventas.venta.entity.EstadoVenta;
import pe.edu.upeu.panaderia.ventas.venta.entity.TipoVenta;

public interface VentaService {

    List<VentaResponse> listar(TipoVenta tipo, EstadoVenta estado, LocalDateTime fechaDesde, LocalDateTime fechaHasta,
                               String paisDestino, Long productoId, Long clienteId, String ordenFecha,
                               IdentidadJwt identidad);

    VentaResponse obtener(Long id, IdentidadJwt identidad);

    VentaResponse registrar(VentaRequest request, IdentidadJwt identidad);

    VentaResponse anular(Long id);
}
