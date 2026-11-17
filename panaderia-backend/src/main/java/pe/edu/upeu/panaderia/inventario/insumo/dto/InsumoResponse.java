package pe.edu.upeu.panaderia.inventario.insumo.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class InsumoResponse {

    private Long id;
    private String nombre;
    private String unidadMedida;
    private BigDecimal stockActual;
    private BigDecimal stockMinimo;
}