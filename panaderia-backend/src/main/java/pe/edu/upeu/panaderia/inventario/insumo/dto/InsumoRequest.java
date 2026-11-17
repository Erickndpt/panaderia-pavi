package pe.edu.upeu.panaderia.inventario.insumo.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InsumoRequest {

    @NotBlank
    @Size(max = 80)
    private String nombre;

    @NotBlank
    @Size(max = 20)
    private String unidadMedida;

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 9, fraction = 3)
    private BigDecimal stockActual;

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 9, fraction = 3)
    private BigDecimal stockMinimo;
}