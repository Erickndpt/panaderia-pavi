package pe.edu.upeu.panaderia.produccion.orden.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DetalleConsumoRequest(
        @NotNull Long insumoId,
        @NotNull @Positive @Digits(integer = 9, fraction = 3) BigDecimal cantidadConsumida) {
}