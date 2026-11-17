package pe.edu.upeu.panaderia.produccion.orden.controller;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.panaderia.produccion.orden.dto.OrdenProduccionRequest;
import pe.edu.upeu.panaderia.produccion.orden.dto.OrdenProduccionResponse;
import pe.edu.upeu.panaderia.produccion.orden.service.OrdenProduccionService;

@RestController
@RequestMapping("/api/v1/ordenes-produccion")
@RequiredArgsConstructor
@Tag(name = "Producción")
public class OrdenProduccionController {

    private final OrdenProduccionService ordenProduccionService;

    @PostMapping
    @Operation(summary = "Registrar orden de producción",
            description = "Registra una orden de producción ya completada: descuenta los insumos consumidos,"
                    + " guarda la orden y repone las existencias del producto terminado."
                    + " La fecha, el estado y los nombres históricos se calculan en el servidor.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Orden de producción registrada, insumos descontados y existencias repuestas"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida: insumos vacíos, insumo duplicado o cantidades incorrectas"),
            @ApiResponse(responseCode = "404", description = "Producto o insumo inexistente"),
            @ApiResponse(responseCode = "409", description = "Stock insuficiente de un insumo o conflicto de integridad")
    })
    public ResponseEntity<OrdenProduccionResponse> registrar(@Valid @RequestBody OrdenProduccionRequest request) {
        OrdenProduccionResponse response = ordenProduccionService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar órdenes de producción",
            description = "Devuelve todas las órdenes de producción registradas en el sistema.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Órdenes de producción encontradas")
    })
    public ResponseEntity<List<OrdenProduccionResponse>> listar() {
        return ResponseEntity.ok(ordenProduccionService.listar());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener orden de producción por id",
            description = "Devuelve una orden de producción según su identificador.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orden de producción encontrada"),
            @ApiResponse(responseCode = "404", description = "Orden de producción inexistente")
    })
    public ResponseEntity<OrdenProduccionResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ordenProduccionService.obtener(id));
    }
}