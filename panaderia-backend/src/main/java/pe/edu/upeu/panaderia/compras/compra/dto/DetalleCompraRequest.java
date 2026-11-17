package pe.edu.upeu.panaderia.compras.compra.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DetalleCompraRequest(
        @NotNull Long insumoId,
        @NotNull @Positive BigDecimal cantidad,
        @NotNull @DecimalMin("0.00") BigDecimal precioUnitario) {
}