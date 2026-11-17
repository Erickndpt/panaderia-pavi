package pe.edu.upeu.panaderia.ventas.pago.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.edu.upeu.panaderia.ventas.pago.entity.MetodoPago;

/**
 * Entrada de pago.
 *
 * <p>No expone fecha ni estado a propósito: la fecha la fija el servidor y
 * el estado siempre nace en REGISTRADO, de modo que el cliente no puede
 * registrar un pago con fecha arbitraria ni fingir que está anulado.
 */
public record PagoVentaRequest(
        @NotNull(message = "El método de pago es obligatorio") MetodoPago metodoPago,
        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.00", inclusive = false, message = "El monto debe ser mayor que cero")
        @Digits(integer = 10, fraction = 2, message = "El monto admite hasta 2 decimales")
        BigDecimal monto,
        @Size(max = 100, message = "La referencia admite hasta 100 caracteres") String referencia) {
}