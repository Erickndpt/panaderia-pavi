package pe.edu.upeu.panaderia.inventario.existencia.controller;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.panaderia.inventario.existencia.dto.ExistenciaRequest;
import pe.edu.upeu.panaderia.inventario.existencia.dto.ExistenciaResponse;
import pe.edu.upeu.panaderia.inventario.existencia.service.ExistenciaService;

@RestController
@RequestMapping("/api/v1/existencias")
@RequiredArgsConstructor
@Tag(name = "Existencias")
public class ExistenciaController {

    private final ExistenciaService existenciaService;

    @GetMapping
    @Operation(summary = "Listar existencias",
            description = "Devuelve todas las existencias registradas en el sistema. "
                    + "El filtro productoId es opcional y se aplica en el backend.")
    public ResponseEntity<List<ExistenciaResponse>> listar(
            @RequestParam(required = false) Long productoId) {
        return ResponseEntity.ok(existenciaService.listar(productoId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener existencia por id", description = "Devuelve una existencia según su identificador.")
    public ResponseEntity<ExistenciaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(existenciaService.obtener(id));
    }

    @GetMapping("/producto/{productoId}")
    @Operation(summary = "Obtener existencia por producto", description = "Devuelve la existencia de un producto según su identificador.")
    public ResponseEntity<ExistenciaResponse> obtenerPorProducto(@PathVariable Long productoId) {
        return ResponseEntity.ok(existenciaService.obtenerPorProducto(productoId));
    }

    @PostMapping
    @Operation(summary = "Crear existencia",
            description = "Registra la existencia de un producto. Un producto no puede tener más de una existencia.")
    public ResponseEntity<ExistenciaResponse> crear(@Valid @RequestBody ExistenciaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(existenciaService.crear(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar existencia",
            description = "Actualiza la cantidad y el producto asociado de una existencia existente.")
    public ResponseEntity<ExistenciaResponse> actualizar(@PathVariable Long id,
                                                        @Valid @RequestBody ExistenciaRequest request) {
        return ResponseEntity.ok(existenciaService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar existencia",
            description = "Elimina una existencia solo si su cantidad es exactamente cero.")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        existenciaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
