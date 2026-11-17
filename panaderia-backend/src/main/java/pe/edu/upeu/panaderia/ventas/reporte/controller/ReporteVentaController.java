package pe.edu.upeu.panaderia.ventas.reporte.controller;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.panaderia.ventas.reporte.dto.ReporteVentaResponse;
import pe.edu.upeu.panaderia.ventas.reporte.service.ReporteVentaService;

@RestController
@RequestMapping("/api/v1/reportes/ventas")
@RequiredArgsConstructor
@Tag(name = "Reportes")
public class ReporteVentaController {

    private final ReporteVentaService reporteVentaService;

    @GetMapping
    @Operation(summary = "Reporte resumido de ventas",
            description = "Devuelve la cantidad y el monto total de ventas, separando locales y exportaciones;"
                    + " puede filtrarse por fechaDesde y fechaHasta (ISO-8601, opcionales).")
    public ResponseEntity<ReporteVentaResponse> generar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHasta) {
        return ResponseEntity.ok(reporteVentaService.generar(fechaDesde, fechaHasta));
    }
}