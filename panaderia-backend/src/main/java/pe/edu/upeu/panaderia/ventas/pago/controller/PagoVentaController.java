package pe.edu.upeu.panaderia.ventas.pago.controller;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.panaderia.IdentidadJwt;
import pe.edu.upeu.panaderia.ventas.pago.dto.PagoVentaRequest;
import pe.edu.upeu.panaderia.ventas.pago.dto.PagoVentaResponse;
import pe.edu.upeu.panaderia.ventas.pago.dto.PagoVentaResumenResponse;
import pe.edu.upeu.panaderia.ventas.pago.service.PagoVentaService;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Pagos de venta")
public class PagoVentaController {

    private final PagoVentaService pagoVentaService;

    @PostMapping("/ventas/{ventaId}/pagos")
    @Operation(summary = "Registrar pago de una venta",
            description = "Registra un pago contra la venta indicada. La fecha y el estado los fija el servidor"
                    + " (REGISTRADO). No se admiten pagos sobre una venta ANULADA ni por un monto mayor que el saldo"
                    + " pendiente, contando solo pagos REGISTRADO.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Pago registrado"),
            @ApiResponse(responseCode = "400", description = "Monto no positivo o venta anulada"),
            @ApiResponse(responseCode = "404", description = "Venta inexistente"),
            @ApiResponse(responseCode = "409", description = "El pago excede el saldo pendiente")
    })
    public ResponseEntity<PagoVentaResponse> registrar(@PathVariable Long ventaId,
                                                        @Valid @RequestBody PagoVentaRequest request,
                                                        @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pagoVentaService.registrar(ventaId, request, IdentidadJwt.desde(jwt)));
    }

    @GetMapping("/ventas/{ventaId}/pagos")
    @Operation(summary = "Listar pagos de una venta",
            description = "Devuelve los pagos de la venta, incluidos los anulados, ordenados por id.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pagos de la venta"),
            @ApiResponse(responseCode = "404", description = "Venta inexistente")
    })
    public ResponseEntity<List<PagoVentaResponse>> listarPorVenta(@PathVariable Long ventaId,
                                                                 @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(pagoVentaService.listarPorVenta(ventaId, IdentidadJwt.desde(jwt)));
    }

    @GetMapping("/ventas/{ventaId}/pagos/resumen")
    @Operation(summary = "Resumen de pagos de una venta",
            description = "Total de la venta, total pagado (solo pagos REGISTRADO), saldo pendiente y situación"
                    + " PENDIENTE, PARCIAL o PAGADO.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resumen de cobros"),
            @ApiResponse(responseCode = "404", description = "Venta inexistente")
    })
    public ResponseEntity<PagoVentaResumenResponse> resumen(@PathVariable Long ventaId,
                                                           @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(pagoVentaService.resumen(ventaId, IdentidadJwt.desde(jwt)));
    }

    @GetMapping("/pagos-venta/{pagoId}")
    @Operation(summary = "Obtener pago por id", description = "Devuelve un pago según su identificador.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pago encontrado"),
            @ApiResponse(responseCode = "404", description = "Pago inexistente")
    })
    public ResponseEntity<PagoVentaResponse> obtener(@PathVariable Long pagoId,
                                                     @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(pagoVentaService.obtener(pagoId, IdentidadJwt.desde(jwt)));
    }

    @PatchMapping("/pagos-venta/{pagoId}/anular")
    @Operation(summary = "Anular pago",
            description = "Cambia el estado del pago a ANULADO y lo excluye del saldo. No elimina el registro ni"
                    + " repone stock. No acepta cuerpo de solicitud.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Pago anulado"),
            @ApiResponse(responseCode = "404", description = "Pago inexistente"),
            @ApiResponse(responseCode = "409", description = "El pago ya estaba anulado")
    })
    public ResponseEntity<PagoVentaResponse> anular(@PathVariable Long pagoId,
                                                    @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(pagoVentaService.anular(pagoId, IdentidadJwt.desde(jwt)));
    }
}