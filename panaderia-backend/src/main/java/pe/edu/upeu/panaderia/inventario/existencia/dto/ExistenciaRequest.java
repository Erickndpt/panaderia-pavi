package pe.edu.upeu.panaderia.inventario.existencia.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExistenciaRequest {

    @NotNull
    @Positive
    private Long productoId;

    @NotNull
    @DecimalMin("0.000")
    @Digits(integer = 9, fraction = 3)
    private BigDecimal cantidad;
}
