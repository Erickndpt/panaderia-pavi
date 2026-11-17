package pe.edu.upeu.panaderia.finanzas.movimiento.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.panaderia.exception.GlobalExceptionHandler.ErrorResponse;
import pe.edu.upeu.panaderia.finanzas.movimiento.dto.MovimientoFinancieroResponse;
import pe.edu.upeu.panaderia.finanzas.movimiento.service.FinanzasService;

@RestController
@RequestMapping("/api/v1/movimientos-financieros")
@RequiredArgsConstructor
@Tag(name = "Finanzas")
public class MovimientoFinancieroController {

    private final FinanzasService finanzasService;

    @GetMapping
    @Operation(summary = "Listar movimientos financieros",
            description = "Devuelve todos los movimientos financieros (ingresos y egresos) "
                    + "derivados automáticamente de las ventas y compras registradas.")
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                description = "Listado de movimientos financieros",
                content = @Content(schema = @Schema(implementation = MovimientoFinancieroResponse.class))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Ya existe un movimiento financiero para el mismo origen",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<MovimientoFinancieroResponse>> listar() {
        return ResponseEntity.ok(finanzasService.listar());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener movimiento financiero por id",
            description = "Devuelve el movimiento financiero cuyo id coincide; "
                    + "lanza 404 si no existe.")
    @ApiResponses({
        @ApiResponse(responseCode = "200",
                description = "Movimiento financiero encontrado",
                content = @Content(schema = @Schema(implementation = MovimientoFinancieroResponse.class))),
        @ApiResponse(responseCode = "404", description = "Movimiento financiero no encontrado",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MovimientoFinancieroResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(finanzasService.obtener(id));
    }
}