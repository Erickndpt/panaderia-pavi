package pe.edu.upeu.panaderia.produccion.orden.dto;

import java.math.BigDecimal;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrdenProduccionRequest(
        @NotNull Long productoId,
        @NotNull @Positive @Digits(integer = 9, fraction = 3) BigDecimal cantidadProducida,
        @NotEmpty @Valid List<DetalleConsumoRequest> insumos) {
}