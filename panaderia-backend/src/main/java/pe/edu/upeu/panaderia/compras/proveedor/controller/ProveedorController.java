package pe.edu.upeu.panaderia.compras.proveedor.controller;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import pe.edu.upeu.panaderia.compras.proveedor.dto.ProveedorRequest;
import pe.edu.upeu.panaderia.compras.proveedor.dto.ProveedorResponse;
import pe.edu.upeu.panaderia.compras.proveedor.service.ProveedorService;

@RestController
@RequestMapping("/api/v1/proveedores")
@RequiredArgsConstructor
@Tag(name = "Proveedores")
public class ProveedorController {

    private final ProveedorService proveedorService;

    @GetMapping
    @Operation(summary = "Listar proveedores", description = "Devuelve todos los proveedores registrados en el sistema.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Proveedores encontrados")
    })
    public ResponseEntity<List<ProveedorResponse>> listar() {
        return ResponseEntity.ok(proveedorService.listar());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener proveedor por id", description = "Devuelve un proveedor según su identificador.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Proveedor encontrado"),
            @ApiResponse(responseCode = "404", description = "Proveedor inexistente")
    })
    public ResponseEntity<ProveedorResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(proveedorService.obtener(id));
    }

    @PostMapping
    @Operation(summary = "Crear proveedor", description = "Registra un nuevo proveedor en el sistema.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Proveedor creado"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida: RUC, razón social u otros datos incorrectos"),
            @ApiResponse(responseCode = "409", description = "El RUC ya está registrado o existe un conflicto de integridad")
    })
    public ResponseEntity<ProveedorResponse> crear(@Valid @RequestBody ProveedorRequest request) {
        ProveedorResponse response = proveedorService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar proveedor", description = "Actualiza un proveedor existente según su identificador.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Proveedor actualizado"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "404", description = "Proveedor inexistente"),
            @ApiResponse(responseCode = "409", description = "El RUC pertenece a otro proveedor o existe un conflicto de integridad")
    })
    public ResponseEntity<ProveedorResponse> actualizar(@PathVariable Long id,
                                                        @Valid @RequestBody ProveedorRequest request) {
        return ResponseEntity.ok(proveedorService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar proveedor", description = "Elimina un proveedor según su identificador.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Proveedor eliminado"),
            @ApiResponse(responseCode = "404", description = "Proveedor inexistente"),
            @ApiResponse(responseCode = "409", description = "Existe un conflicto de integridad")
    })
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        proveedorService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}