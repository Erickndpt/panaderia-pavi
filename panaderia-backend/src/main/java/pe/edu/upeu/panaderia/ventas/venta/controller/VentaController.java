package pe.edu.upeu.panaderia.ventas.venta.controller;

import java.time.LocalDateTime;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.panaderia.IdentidadJwt;
import pe.edu.upeu.panaderia.ventas.reporte.dto.ReporteVentaResponse;
import pe.edu.upeu.panaderia.ventas.reporte.service.ReporteVentaService;
import pe.edu.upeu.panaderia.ventas.venta.dto.VentaRequest;
import pe.edu.upeu.panaderia.ventas.venta.dto.VentaResponse;
import pe.edu.upeu.panaderia.ventas.venta.entity.EstadoVenta;
import pe.edu.upeu.panaderia.ventas.venta.entity.TipoVenta;
import pe.edu.upeu.panaderia.ventas.venta.service.VentaService;

@RestController
@RequestMapping("/api/v1/ventas")
@RequiredArgsConstructor
@Tag(name = "Ventas")
public class VentaController {

    private final VentaService ventaService;
    private final ReporteVentaService reporteVentaService;

    @GetMapping
    @Operation(summary = "Listar ventas",
            description = "Devuelve las ventas registradas; puede filtrarse por tipo (LOCAL o EXPORTACION),"
                    + " estado (REGISTRADA o ANULADA), fechaDesde, fechaHasta (ISO-8601), paisDestino (búsqueda"
                    + " parcial sin distinguir mayúsculas y minúsculas), productoId y clienteId. Se ordena por"
                    + " fecha según ordenFecha (ASC: más antiguas primero, DESC: más recientes primero; por"
                    + " defecto DESC). Todos los parámetros son opcionales y combinables.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ventas filtradas y ordenadas"),
            @ApiResponse(responseCode = "400", description = "Filtros inválidos")
    })
    public ResponseEntity<List<VentaResponse>> listar(
            @RequestParam(required = false) TipoVenta tipo,
            @RequestParam(required = false) EstadoVenta estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHasta,
            @RequestParam(required = false) String paisDestino,
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false, defaultValue = "DESC") String ordenFecha,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ventaService.listar(
                tipo, estado, fechaDesde, fechaHasta, paisDestino, productoId, clienteId, ordenFecha,
                IdentidadJwt.desde(jwt)));
    }

    @GetMapping("/resumen")
    @Operation(summary = "Resumen de ventas",
            description = "Contabiliza únicamente las ventas REGISTRADA (las ANULADA quedan excluidas de cantidades"
                    + " y montos), separando locales y exportaciones; puede filtrarse por fechaDesde y fechaHasta"
                    + " (ISO-8601, opcionales). Devuelve el mismo ReporteVentaResponse que GET /api/v1/reportes/ventas.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resumen de ventas REGISTRADA"),
            @ApiResponse(responseCode = "400", description = "Filtros de fecha inválidos")
    })
    public ResponseEntity<ReporteVentaResponse> resumen(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHasta) {
        return ResponseEntity.ok(reporteVentaService.generar(fechaDesde, fechaHasta));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener venta por id", description = "Devuelve una venta según su identificador.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Venta encontrada"),
            @ApiResponse(responseCode = "404", description = "Venta inexistente")
    })
    public ResponseEntity<VentaResponse> obtener(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ventaService.obtener(id, IdentidadJwt.desde(jwt)));
    }

    @PostMapping
    @Operation(summary = "Registrar venta",
            description = "Registra una nueva venta contra el cliente indicado en clienteId y descuenta el stock de"
                    + " cada producto. El cliente debe existir y estar activo.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Venta registrada"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "404", description = "Cliente, producto o existencia inexistente"),
            @ApiResponse(responseCode = "409", description = "Stock insuficiente o cliente inactivo")
    })
    public ResponseEntity<VentaResponse> registrar(@Valid @RequestBody VentaRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        VentaResponse response = ventaService.registrar(request, IdentidadJwt.desde(jwt));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/anular")
    @Operation(summary = "Anular venta",
            description = "Anula una venta REGISTRADA: repone el stock de cada detalle y cambia el estado a ANULADA."
                    + " No elimina la venta ni sus detalles. No acepta cuerpo de solicitud.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Venta anulada"),
            @ApiResponse(responseCode = "404", description = "Venta o existencia inexistente"),
            @ApiResponse(responseCode = "409", description = "Venta ya anulada o conflicto")
    })
    public ResponseEntity<VentaResponse> anular(@PathVariable Long id) {
        return ResponseEntity.ok(ventaService.anular(id));
    }
}