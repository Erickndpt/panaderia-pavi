package pe.edu.upeu.panaderia.inventario.insumo.controller;

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
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.panaderia.inventario.insumo.dto.InsumoRequest;
import pe.edu.upeu.panaderia.inventario.insumo.dto.InsumoResponse;
import pe.edu.upeu.panaderia.inventario.insumo.service.InsumoService;

@RestController
@RequestMapping("/api/v1/insumos")
@RequiredArgsConstructor
@Tag(name = "Insumos")
public class InsumoController {

    private final InsumoService insumoService;

    @GetMapping
    @Operation(summary = "Listar insumos", description = "Devuelve todos los insumos registrados en el sistema.")
    public ResponseEntity<List<InsumoResponse>> listar() {
        return ResponseEntity.ok(insumoService.listar());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener insumo por id", description = "Devuelve un insumo según su identificador.")
    public ResponseEntity<InsumoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(insumoService.obtener(id));
    }

    @PostMapping
    @Operation(summary = "Crear insumo", description = "Registra un nuevo insumo (materia prima) en el sistema.")
    public ResponseEntity<InsumoResponse> crear(@Valid @RequestBody InsumoRequest request) {
        InsumoResponse response = insumoService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar insumo", description = "Actualiza un insumo existente según su identificador.")
    public ResponseEntity<InsumoResponse> actualizar(@PathVariable Long id,
                                                     @Valid @RequestBody InsumoRequest request) {
        return ResponseEntity.ok(insumoService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar insumo", description = "Elimina un insumo según su identificador.")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        insumoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}