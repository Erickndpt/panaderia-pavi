package pe.edu.upeu.panaderia.compras.compra.dto;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CompraRequest(
        @NotNull Long proveedorId,
        @NotEmpty @Valid List<DetalleCompraRequest> detalles) {
}