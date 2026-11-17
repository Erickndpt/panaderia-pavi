package pe.edu.upeu.panaderia.administracion.producto.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import pe.edu.upeu.panaderia.administracion.categoria.dto.CategoriaResumen;

@Getter
@Setter
@Builder
public class ProductoResponse {

    private Long id;
    private String nombre;
    private BigDecimal precio;
    private CategoriaResumen categoria;
}