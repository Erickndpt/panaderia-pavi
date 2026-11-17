package pe.edu.upeu.panaderia.finanzas.movimiento.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.panaderia.exception.GlobalExceptionHandler.ErrorResponse;
import pe.edu.upeu.panaderia.finanzas.movimiento.dto.ResumenFinancieroResponse;
import pe.edu.upeu.panaderia.finanzas.movimiento.service.FinanzasService;

@RestController
@RequestMapping("/api/v1/reportes/finanzas")
@RequiredArgsConstructor
@Tag(name = "Reportes")
public class ReporteFinanzasController {

    private final FinanzasService finanzasService;

    @GetMapping
    @Operation(summary = "Reporte financiero",
            description = "Devuelve la cantidad y el monto de los movimientos activos, "
                    + "separando ingresos y egresos, y el saldo resultante "
                    + "(monto de ingresos menos monto de egresos). Los movimientos anulados se excluyen.")
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                description = "Resumen financiero de movimientos activos",
                content = @Content(schema = @Schema(implementation = ResumenFinancieroResponse.class)))
    })
    public ResponseEntity<ResumenFinancieroResponse> resumen() {
        return ResponseEntity.ok(finanzasService.resumen());
    }
}