package pe.edu.upeu.panaderia.compras.compra.controller;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.panaderia.compras.compra.dto.CompraRequest;
import pe.edu.upeu.panaderia.compras.compra.dto.CompraResponse;
import pe.edu.upeu.panaderia.compras.compra.service.CompraService;

@RestController
@RequestMapping("/api/v1/compras")
@RequiredArgsConstructor
@Tag(name = "Compras")
public class CompraController {

    private final CompraService compraService;

    @PostMapping
    @Operation(summary = "Registrar compra",
            description = "Registra una nueva compra de insumos a un proveedor y aumenta el stock de cada insumo."
                    + " El total, los subtotales, la fecha y el estado se calculan en el servidor.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Compra registrada y stock aumentado"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida: detalles vacíos o datos incorrectos"),
            @ApiResponse(responseCode = "404", description = "Proveedor o insumo inexistente"),
            @ApiResponse(responseCode = "409", description = "Proveedor inactivo o conflicto de integridad")
    })
    public ResponseEntity<CompraResponse> registrar(@Valid @RequestBody CompraRequest request) {
        CompraResponse response = compraService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar compras", description = "Devuelve todas las compras registradas en el sistema.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Compras encontradas")
    })
    public ResponseEntity<List<CompraResponse>> listar() {
        return ResponseEntity.ok(compraService.listar());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener compra por id", description = "Devuelve una compra según su identificador.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Compra encontrada"),
            @ApiResponse(responseCode = "404", description = "Compra inexistente")
    })
    public ResponseEntity<CompraResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(compraService.obtener(id));
    }

    @PatchMapping("/{id}/anular")
    @Operation(summary = "Anular compra",
            description = "Anula una compra REGISTRADA: descuenta del stock de cada insumo la cantidad de sus"
                    + " detalles, marca el egreso financiero asociado como ANULADO y cambia el estado a ANULADA."
                    + " No elimina la compra, sus detalles ni el movimiento; se conservan como histórico."
                    + " No acepta cuerpo de solicitud.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Compra anulada"),
            @ApiResponse(responseCode = "404", description = "Compra o movimiento financiero inexistente"),
            @ApiResponse(responseCode = "409", description = "Compra ya anulada o stock insuficiente")
    })
    public ResponseEntity<CompraResponse> anular(@PathVariable Long id) {
        return ResponseEntity.ok(compraService.anular(id));
    }
}