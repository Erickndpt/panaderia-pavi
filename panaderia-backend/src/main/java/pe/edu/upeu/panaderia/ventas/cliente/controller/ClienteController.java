package pe.edu.upeu.panaderia.ventas.cliente.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.panaderia.ventas.cliente.dto.ClienteRequest;
import pe.edu.upeu.panaderia.ventas.cliente.dto.ClienteResponse;
import pe.edu.upeu.panaderia.ventas.cliente.service.ClienteService;

@RestController
@RequestMapping("/api/v1/clientes")
@RequiredArgsConstructor
@Tag(name = "Clientes")
public class ClienteController {

    private final ClienteService clienteService;

    @GetMapping
    @Operation(summary = "Listar clientes",
            description = "Devuelve los clientes registrados; puede filtrarse por buscar, que compara de forma"
                    + " parcial y sin distinguir mayúsculas y minúsculas contra el nombre o razón social y contra el"
                    + " número de documento. El parámetro es opcional.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Clientes encontrados")
    })
    public ResponseEntity<List<ClienteResponse>> listar(@RequestParam(required = false) String buscar) {
        return ResponseEntity.ok(clienteService.listar(buscar));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener cliente por id", description = "Devuelve un cliente según su identificador.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
            @ApiResponse(responseCode = "404", description = "Cliente inexistente")
    })
    public ResponseEntity<ClienteResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.obtener(id));
    }

    @PostMapping
    @Operation(summary = "Crear cliente", description = "Registra un nuevo cliente en el sistema. El número de"
            + " documento es único y su formato depende del tipo: DNI de 8 dígitos, RUC de 11 dígitos, CE o PASAPORTE"
            + " de 3 a 20 caracteres, y SIN_DOCUMENTO sin número de documento.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Cliente creado"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida: tipo de documento, nombre u otros datos incorrectos"),
            @ApiResponse(responseCode = "409", description = "El número de documento ya está registrado o existe un conflicto de integridad")
    })
    public ResponseEntity<ClienteResponse> crear(@Valid @RequestBody ClienteRequest request) {
        ClienteResponse response = clienteService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar cliente", description = "Actualiza un cliente existente según su identificador.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cliente actualizado"),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @ApiResponse(responseCode = "404", description = "Cliente inexistente"),
            @ApiResponse(responseCode = "409", description = "El número de documento pertenece a otro cliente o existe un conflicto de integridad")
    })
    public ResponseEntity<ClienteResponse> actualizar(@PathVariable Long id,
                                                      @Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.ok(clienteService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar cliente",
            description = "Elimina un cliente según su identificador. Si el cliente tiene ventas asociadas la"
                    + " eliminación se rechaza: las ventas nunca se eliminan.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Cliente eliminado"),
            @ApiResponse(responseCode = "404", description = "Cliente inexistente"),
            @ApiResponse(responseCode = "409", description = "El cliente tiene ventas asociadas")
    })
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        clienteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
